package com.psybergate.acceptance.domain.auth;

import com.psybergate.acceptance.drivers.ui.pages.LoginPage;
import com.psybergate.acceptance.world.TestWorld;
import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

@Component
@ScenarioScope
public class LoginAssertions {

	private final LoginPage loginPage;
	private final TestWorld testWorld;

	public LoginAssertions(LoginPage loginPage, TestWorld testWorld) {
		this.loginPage = loginPage;
		this.testWorld = testWorld;
	}

	public void assertRedirectedToHome() {
		assertThat(loginPage.isOnLoginPage())
			.as("Expected the user to be redirected away from the login page")
			.isFalse();
	}

	public void assertOnLoginPage() {
		assertThat(loginPage.isOnLoginPage())
			.as("Expected the user to remain on the login page")
			.isTrue();
	}

	public void assertRemainsAuthenticatedAfterReload() {
		assertThat(loginPage.isOnLoginPage())
			.as("Expected the user to remain authenticated and not be redirected to the login page after reload")
			.isFalse();
	}

	public void assertErrorMessageVisible(String expectedMessage) {
		String actualMessage = loginPage.getErrorMessage();
		assertThat(actualMessage)
			.as("Expected error message to be visible with text: %s", expectedMessage)
			.isEqualTo(expectedMessage);
	}
}
