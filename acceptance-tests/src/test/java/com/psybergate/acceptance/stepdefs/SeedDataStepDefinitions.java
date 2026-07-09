package com.psybergate.acceptance.stepdefs;

import com.jayway.jsonpath.JsonPath;
import com.psybergate.acceptance.drivers.api.SeedDataApiDriver;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SeedDataStepDefinitions {

	private final SeedDataApiDriver seedDataApiDriver;
	private HttpResponse<String> response;

	public SeedDataStepDefinitions(SeedDataApiDriver seedDataApiDriver) {
		this.seedDataApiDriver = seedDataApiDriver;
	}

	// --- Background ---

	@Given("the application is running with seed data loaded")
	public void theApplicationIsRunningWithSeedDataLoaded() {
		// No-op — precondition assumed; the app must be running with the local profile.
	}

	// --- When steps ---

	@When("I request all employees")
	public void iRequestAllEmployees() {
		response = seedDataApiDriver.getEmployees();
	}

	@When("I request all companies")
	public void iRequestAllCompanies() {
		response = seedDataApiDriver.getCompanies();
	}

	@When("I request all projects")
	public void iRequestAllProjects() {
		response = seedDataApiDriver.getProjects();
	}

	@When("I request all users")
	public void iRequestAllUsers() {
		response = seedDataApiDriver.getUsers();
	}

	@When("I request all interactions")
	public void iRequestAllInteractions() {
		response = seedDataApiDriver.getInteractions();
	}

	@When("I request all tasks")
	public void iRequestAllTasks() {
		response = seedDataApiDriver.getTasks();
	}

	// --- Then steps ---

	@Then("the response status is {int}")
	public void theResponseStatusIs(int expectedStatus) {
		assertThat(response.statusCode())
			.as("Expected HTTP status %d", expectedStatus)
			.isEqualTo(expectedStatus);
	}

	@Then("the response contains at least {int} records")
	public void theResponseContainsAtLeastRecords(int minCount) {
		List<?> records = JsonPath.read(response.body(), "$");
		assertThat(records)
			.as("Expected at least %d records in response", minCount)
			.hasSizeGreaterThanOrEqualTo(minCount);
	}

	@Then("the response contains at least {int} distinct interaction types")
	public void theResponseContainsAtLeastDistinctInteractionTypes(int minDistinctTypes) {
		List<String> types = JsonPath.read(response.body(), "$[*].type");
		long distinctCount = types.stream().distinct().count();
		assertThat(distinctCount)
			.as("Expected at least %d distinct interaction types", minDistinctTypes)
			.isGreaterThanOrEqualTo(minDistinctTypes);
	}

	@Then("the response contains at least one task with status {string}")
	public void theResponseContainsAtLeastOneTaskWithStatus(String expectedStatus) {
		List<String> statuses = JsonPath.read(response.body(), "$[*].status");
		assertThat(statuses)
			.as("Expected at least one task with status '%s'", expectedStatus)
			.contains(expectedStatus);
	}
}
