package com.psybergate.acceptance.domain.auth;

import com.psybergate.acceptance.drivers.ui.pages.LoginPage;
import com.psybergate.acceptance.world.TestWorld;
import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;

@Component
@ScenarioScope
public class LoginActor {

	private final LoginPage loginPage;
	private final TestWorld testWorld;

	public LoginActor(LoginPage loginPage, TestWorld testWorld) {
		this.loginPage = loginPage;
		this.testWorld = testWorld;
	}

	public void navigateToLogin() {
		loginPage.open();
	}

	public void loginAs(String email, String password) {
		loginPage.open();
		loginPage.fillEmail(email);
		loginPage.fillPassword(password);
		loginPage.submit();
		testWorld.set("currentUserEmail", email);
	}
}
