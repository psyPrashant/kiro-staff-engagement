package com.psybergate.acceptance.stepdefs.scheduling;

import com.psybergate.acceptance.domain.scheduling.NextScheduledActor;
import com.psybergate.acceptance.domain.scheduling.NextScheduledAssertions;
import com.psybergate.acceptance.world.TestWorld;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import javax.sql.DataSource;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

public class NextScheduledStepDefinitions {

	private final NextScheduledActor nextScheduledActor;
	private final NextScheduledAssertions nextScheduledAssertions;
	private final TestWorld testWorld;
	private final DataSource dataSource;

	public NextScheduledStepDefinitions(NextScheduledActor nextScheduledActor,
										NextScheduledAssertions nextScheduledAssertions,
										TestWorld testWorld,
										DataSource dataSource) {
		this.nextScheduledActor = nextScheduledActor;
		this.nextScheduledAssertions = nextScheduledAssertions;
		this.testWorld = testWorld;
		this.dataSource = dataSource;
	}

	// --- Given steps ---

	@Given("a manager has scheduled a {string} interaction for employee {long} on a date {int} days from now")
	public void aManagerHasScheduledInteractionForEmployeeDaysFromNow(String type, Long employeeId, int days) {
		String date = LocalDate.now().plusDays(days).format(DateTimeFormatter.ISO_LOCAL_DATE);
		nextScheduledActor.scheduleInteractionForEmployee(employeeId, date, type);
		testWorld.set("currentEmployeeId", employeeId);
	}

	@Given("a manager has scheduled a {string} interaction for employee {long} on a date {int} days ago")
	public void aManagerHasScheduledInteractionForEmployeeDaysAgo(String type, Long employeeId, int days) {
		String date = LocalDate.now().minusDays(days).format(DateTimeFormatter.ISO_LOCAL_DATE);
		seedPendingInteractionViaSQL(employeeId, LocalDate.parse(date), type);
		testWorld.set("currentEmployeeId", employeeId);
	}

	@Given("employee {long} has no pending scheduled interactions")
	public void employeeHasNoPendingScheduledInteractions(Long employeeId) {
		// No setup needed — assumption that the employee exists but has no pending interactions.
		// Scenario isolation ensures a clean state per scenario.
		testWorld.set("currentEmployeeId", employeeId);
	}

	@Given("employee {long} has no future pending scheduled interactions")
	public void employeeHasNoFuturePendingScheduledInteractions(Long employeeId) {
		// The prior Given step scheduled a past-dated interaction.
		// No additional setup needed — there are no future pending interactions.
		testWorld.set("currentEmployeeId", employeeId);
	}

	// --- When steps ---

	@When("the client requests the employee 360 for employee {long}")
	public void theClientRequestsTheEmployee360ForEmployee(Long employeeId) {
		nextScheduledActor.fetchEmployee360(employeeId);
	}

	@When("the client requests the employees list")
	public void theClientRequestsTheEmployeesList() {
		nextScheduledActor.fetchEmployeesList();
	}

	@When("a manager schedules a {string} interaction for employee {long} on a date {int} days from now")
	public void aManagerSchedulesInteractionForEmployeeDaysFromNow(String type, Long employeeId, int days) {
		String date = LocalDate.now().plusDays(days).format(DateTimeFormatter.ISO_LOCAL_DATE);
		nextScheduledActor.scheduleInteractionForEmployee(employeeId, date, type);
	}

	// --- Then steps ---

	@Then("the response status code is {int}")
	public void theResponseStatusCodeIs(int expectedStatus) {
		HttpResponse<String> response = testWorld.get("lastEmployee360Response");
		if (response == null) {
			response = testWorld.get("lastEmployeesListResponse");
		}
		assertThat(response).as("No response found in TestWorld").isNotNull();
		assertThat(response.statusCode())
			.as("Expected HTTP status %d", expectedStatus)
			.isEqualTo(expectedStatus);
	}

	@Then("the 360 response contains nextScheduled with scheduledAt {int} days from now and type {string}")
	public void the360ResponseContainsNextScheduledWithRelativeDate(int days, String expectedType) {
		HttpResponse<String> response = testWorld.get("lastEmployee360Response");
		String expectedDate = LocalDate.now().plusDays(days).format(DateTimeFormatter.ISO_LOCAL_DATE);
		nextScheduledAssertions.assertNextScheduledEquals(response.body(), expectedDate, expectedType);
	}

	@Then("the 360 response contains nextScheduled as null")
	public void the360ResponseContainsNextScheduledAsNull() {
		HttpResponse<String> response = testWorld.get("lastEmployee360Response");
		nextScheduledAssertions.assertNextScheduledIsNull(response.body());
	}

	@Then("the employees list shows employee {long} with nextScheduled scheduledAt {int} days from now and type {string}")
	public void theEmployeesListShowsEmployeeWithNextScheduled(Long employeeId, int days, String expectedType) {
		HttpResponse<String> response = testWorld.get("lastEmployeesListResponse");
		String expectedDate = LocalDate.now().plusDays(days).format(DateTimeFormatter.ISO_LOCAL_DATE);
		nextScheduledAssertions.assertEmployeeListNextScheduled(response.body(), employeeId, expectedDate, expectedType);
	}

	@Then("the employees list shows employee {long} with nextScheduled as null")
	public void theEmployeesListShowsEmployeeWithNextScheduledAsNull(Long employeeId) {
		HttpResponse<String> response = testWorld.get("lastEmployeesListResponse");
		nextScheduledAssertions.assertEmployeeListNextScheduledIsNull(response.body(), employeeId);
	}

	// --- Helper methods ---

	private void seedPendingInteractionViaSQL(Long employeeId, LocalDate scheduledDate, String type) {
		Long userId = lookupFirstUserId();
		String sql = "INSERT INTO scheduled_interactions "
			+ "(employee_id, scheduled_by_user_id, scheduled_date, interaction_type, completion_status, created_at) "
			+ "VALUES (?, ?, ?, ?, 'PENDING', NOW())";

		try (Connection conn = dataSource.getConnection();
			 PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setLong(1, employeeId);
			ps.setLong(2, userId);
			ps.setDate(3, Date.valueOf(scheduledDate));
			ps.setString(4, type);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException("Failed to seed scheduled interaction for employee " + employeeId, e);
		}
	}

	private Long lookupFirstUserId() {
		String sql = "SELECT id FROM users ORDER BY id LIMIT 1";
		try (Connection conn = dataSource.getConnection();
			 PreparedStatement ps = conn.prepareStatement(sql);
			 ResultSet rs = ps.executeQuery()) {
			if (rs.next()) {
				return rs.getLong("id");
			}
			throw new RuntimeException("No users found in database");
		} catch (SQLException e) {
			throw new RuntimeException("Failed to look up user", e);
		}
	}
}
