package com.psybergate.acceptance.stepdefs.auth;

import com.psybergate.acceptance.domain.auth.LoginActor;
import com.psybergate.acceptance.domain.auth.LoginAssertions;
import com.psybergate.acceptance.world.TestWorld;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class LoginStepDefinitions {

	private final LoginActor loginActor;
	private final LoginAssertions loginAssertions;
	private final TestWorld testWorld;

	public LoginStepDefinitions(LoginActor loginActor, LoginAssertions loginAssertions, TestWorld testWorld) {
		this.loginActor = loginActor;
		this.loginAssertions = loginAssertions;
		this.testWorld = testWorld;
	}

	@Given("the user navigates to the login page")
	public void theUserNavigatesToTheLoginPage() {
		loginActor.navigateToLogin();
	}

	@When("the user logs in with email {string} and password {string}")
	public void theUserLogsInWithEmailAndPassword(String email, String password) {
		loginActor.loginAs(email, password);
	}

	@Then("the user should be redirected to the home page")
	public void theUserShouldBeRedirectedToTheHomePage() {
		loginAssertions.assertRedirectedToHome();
	}

	@Then("the user should see the error message {string}")
	public void theUserShouldSeeTheErrorMessage(String errorMessage) {
		loginAssertions.assertErrorMessageVisible(errorMessage);
	}

	@And("the user should remain on the login page")
	public void theUserShouldRemainOnTheLoginPage() {
		loginAssertions.assertOnLoginPage();
	}
}
