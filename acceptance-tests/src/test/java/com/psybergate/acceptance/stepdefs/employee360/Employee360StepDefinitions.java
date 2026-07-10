package com.psybergate.acceptance.stepdefs.employee360;

import com.psybergate.acceptance.domain.auth.LoginAssertions;
import com.psybergate.acceptance.domain.employee360.Employee360Actor;
import com.psybergate.acceptance.domain.employee360.Employee360Assertions;
import com.psybergate.acceptance.drivers.api.SeedDataApiDriver;
import com.psybergate.acceptance.drivers.ui.pages.Employee360Page;
import com.psybergate.acceptance.world.TestWorld;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class Employee360StepDefinitions {

	private final Employee360Actor employee360Actor;
	private final Employee360Assertions employee360Assertions;
	private final LoginAssertions loginAssertions;
	private final Employee360Page employee360Page;
	private final SeedDataApiDriver seedDataApiDriver;
	private final TestWorld testWorld;

	public Employee360StepDefinitions(Employee360Actor employee360Actor,
			Employee360Assertions employee360Assertions,
			LoginAssertions loginAssertions,
			Employee360Page employee360Page,
			SeedDataApiDriver seedDataApiDriver,
			TestWorld testWorld) {
		this.employee360Actor = employee360Actor;
		this.employee360Assertions = employee360Assertions;
		this.loginAssertions = loginAssertions;
		this.employee360Page = employee360Page;
		this.seedDataApiDriver = seedDataApiDriver;
		this.testWorld = testWorld;
	}

	@When("the user navigates to the employee 360 view for employee {long}")
	public void theUserNavigatesToTheEmployee360ViewForEmployee(Long employeeId) {
		employee360Actor.navigateToEmployee360(employeeId);
	}

	@When("the user navigates to the employee 360 view for employee {long} without logging in")
	public void theUserNavigatesToTheEmployee360ViewForEmployeeWithoutLoggingIn(Long employeeId) {
		employee360Page.open(employeeId);
	}

	@Then("the profile summary should be visible")
	public void theProfileSummaryShouldBeVisible() {
		employee360Assertions.assertProfileSummaryIsVisible();
	}

	@Then("the interaction history should be visible")
	public void theInteractionHistoryShouldBeVisible() {
		employee360Assertions.assertInteractionHistoryIsVisible();
	}

	@Then("the open tasks should be visible")
	public void theOpenTasksShouldBeVisible() {
		employee360Assertions.assertOpenTasksAreVisible();
	}

	@Then("the empty interactions message should be displayed")
	public void theEmptyInteractionsMessageShouldBeDisplayed() {
		employee360Assertions.assertEmptyInteractionsMessageShown();
	}

	@Then("the empty tasks message should be displayed")
	public void theEmptyTasksMessageShouldBeDisplayed() {
		employee360Assertions.assertEmptyTasksMessageShown();
	}

	@Then("overdue tasks should be visually distinguished from non-overdue tasks")
	public void overdueTasksShouldBeVisuallyDistinguishedFromNonOverdueTasks() {
		employee360Assertions.assertOverdueTasksAreDistinguished();
	}

	@Then("the user should be redirected to the login page")
	public void theUserShouldBeRedirectedToTheLoginPage() {
		loginAssertions.assertOnLoginPage();
	}
}
