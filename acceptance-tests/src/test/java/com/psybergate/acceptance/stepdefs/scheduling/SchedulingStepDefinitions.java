package com.psybergate.acceptance.stepdefs.scheduling;

import com.psybergate.acceptance.domain.scheduling.SchedulingActor;
import com.psybergate.acceptance.domain.scheduling.SchedulingAssertions;
import com.psybergate.acceptance.drivers.ui.pages.InteractionMatrixPage;
import com.psybergate.acceptance.drivers.ui.pages.ScheduleCalendarPage;
import com.psybergate.acceptance.drivers.ui.pages.ScheduleFormPage;
import com.psybergate.acceptance.world.TestWorld;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class SchedulingStepDefinitions {

	private final SchedulingActor schedulingActor;
	private final SchedulingAssertions schedulingAssertions;
	private final InteractionMatrixPage matrixPage;
	private final ScheduleCalendarPage calendarPage;
	private final ScheduleFormPage formPage;
	private final DataSource dataSource;
	private final TestWorld testWorld;

	public SchedulingStepDefinitions(SchedulingActor schedulingActor,
									 SchedulingAssertions schedulingAssertions,
									 InteractionMatrixPage matrixPage,
									 ScheduleCalendarPage calendarPage,
									 ScheduleFormPage formPage,
									 DataSource dataSource,
									 TestWorld testWorld) {
		this.schedulingActor = schedulingActor;
		this.schedulingAssertions = schedulingAssertions;
		this.matrixPage = matrixPage;
		this.calendarPage = calendarPage;
		this.formPage = formPage;
		this.dataSource = dataSource;
		this.testWorld = testWorld;
	}

	// --- Scenario 1: Schedule from interaction matrix ---

	@Given("the user is on the interaction matrix")
	public void theUserIsOnTheInteractionMatrix() {
		matrixPage.open();
		matrixPage.waitForMatrixLoaded();
	}

	@When("the user clicks {string} for employee {string}")
	public void theUserClicksScheduleNextForEmployee(String buttonLabel, String employeeName) {
		schedulingActor.scheduleNextFromMatrix(employeeName);
	}

	@And("the user sets the scheduled date to a future date")
	public void theUserSetsTheScheduledDateToAFutureDate() {
		LocalDate futureDate = LocalDate.now().plusDays(7);
		String formattedDate = futureDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
		formPage.setScheduledDate(formattedDate);
		testWorld.set("scheduledDate", formattedDate);
	}

	@And("the user selects interaction type {string}")
	public void theUserSelectsInteractionType(String type) {
		formPage.selectInteractionType(type);
	}

	@And("the user submits the schedule form")
	public void theUserSubmitsTheScheduleForm() {
		schedulingActor.submitScheduleForm();
	}

	@Then("the calendar view should display an entry for {string} with the scheduled date")
	public void theCalendarViewShouldDisplayAnEntryForEmployeeWithTheScheduledDate(String employeeName) {
		schedulingActor.navigateToCalendar();
		schedulingAssertions.assertEntryVisible(employeeName);
	}

	// --- Scenario 2: Mark as completed ---

	@Given("the user has a pending scheduled interaction for {string}")
	public void theUserHasAPendingScheduledInteractionFor(String employeeName) {
		// Create a pending interaction via the UI: navigate to matrix, schedule one
		matrixPage.open();
		matrixPage.waitForMatrixLoaded();
		schedulingActor.scheduleNextFromMatrix(employeeName);

		LocalDate futureDate = LocalDate.now().plusDays(3);
		String formattedDate = futureDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
		formPage.setScheduledDate(formattedDate);
		formPage.selectInteractionType("CHECK_IN");
		schedulingActor.submitScheduleForm();
	}

	@And("the user navigates to the schedule calendar")
	public void theUserNavigatesToTheScheduleCalendar() {
		schedulingActor.navigateToCalendar();
	}

	@When("the user expands the entry for {string}")
	public void theUserExpandsTheEntryFor(String employeeName) {
		calendarPage.expandEntry(employeeName);
	}

	@And("the user clicks complete")
	public void theUserClicksComplete() {
		calendarPage.clickComplete();
	}

	@Then("the entry for {string} should no longer appear in the pending list")
	public void theEntryForEmployeeShouldNoLongerAppearInThePendingList(String employeeName) {
		schedulingAssertions.assertEntryNotVisible(employeeName);
	}

	// --- Scenario 3: Overdue via SQL seed ---

	@Given("a past-dated pending scheduled interaction exists for {string} via SQL seed")
	public void aPastDatedPendingScheduledInteractionExistsForViaSqlSeed(String employeeName) {
		Long employeeId = lookupEmployeeIdByName(employeeName);
		Long userId = lookupUserIdByEmail("admin@psybergate.co.za");

		String sql = "INSERT INTO scheduled_interactions "
			+ "(employee_id, scheduled_by_user_id, scheduled_date, interaction_type, completion_status, created_at) "
			+ "VALUES (?, ?, ?, 'CHECK_IN', 'PENDING', NOW())";

		LocalDate pastDate = LocalDate.now().minusDays(7);

		try (Connection conn = dataSource.getConnection();
			 PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setLong(1, employeeId);
			ps.setLong(2, userId);
			ps.setDate(3, Date.valueOf(pastDate));
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException("Failed to seed overdue scheduled interaction for " + employeeName, e);
		}
	}

	@Then("the entry for {string} should have an overdue indicator")
	public void theEntryForEmployeeShouldHaveAnOverdueIndicator(String employeeName) {
		schedulingAssertions.assertOverdueIndicator(employeeName);
	}

	// --- Scenario 4: Past date validation ---

	@Given("the user is on the schedule form for employee {string}")
	public void theUserIsOnTheScheduleFormForEmployee(String employeeName) {
		Long employeeId = lookupEmployeeIdByName(employeeName);
		formPage.open(employeeId);
	}

	@When("the user sets the scheduled date to a past date")
	public void theUserSetsTheScheduledDateToAPastDate() {
		LocalDate pastDate = LocalDate.now().minusDays(3);
		String formattedDate = pastDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
		formPage.setScheduledDate(formattedDate);
	}

	@Then("a date validation error should be displayed")
	public void aDateValidationErrorShouldBeDisplayed() {
		schedulingAssertions.assertDateValidationError();
	}

	@And("the submit button should be disabled")
	public void theSubmitButtonShouldBeDisabled() {
		schedulingAssertions.assertSubmitDisabled();
	}

	// --- Helper methods ---

	private Long lookupEmployeeIdByName(String fullName) {
		String sql = "SELECT id FROM employees WHERE CONCAT(first_name, ' ', last_name) = ?";
		try (Connection conn = dataSource.getConnection();
			 PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, fullName);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getLong("id");
				}
				throw new RuntimeException("Employee not found: " + fullName);
			}
		} catch (SQLException e) {
			throw new RuntimeException("Failed to look up employee: " + fullName, e);
		}
	}

	private Long lookupUserIdByEmail(String email) {
		String sql = "SELECT id FROM users WHERE email = ?";
		try (Connection conn = dataSource.getConnection();
			 PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, email);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getLong("id");
				}
				throw new RuntimeException("User not found: " + email);
			}
		} catch (SQLException e) {
			throw new RuntimeException("Failed to look up user: " + email, e);
		}
	}
}
