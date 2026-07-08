package com.psybergate.acceptance.stepdefs;

import com.psybergate.acceptance.drivers.ui.pages.HomePage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

public class SmokeStepDefinitions {

	private final HomePage homePage;

	public SmokeStepDefinitions(HomePage homePage) {
		this.homePage = homePage;
	}

	@Given("the application is running")
	public void theApplicationIsRunning() {
		// No-op — precondition assumed; the app must be running externally.
	}

	@When("I open the home page")
	public void iOpenTheHomePage() {
		homePage.open();
	}

	@Then("the page loads successfully")
	public void thePageLoadsSuccessfully() {
		assertThat(homePage.isLoaded())
			.as("Expected the home page body to be visible")
			.isTrue();
	}
}
