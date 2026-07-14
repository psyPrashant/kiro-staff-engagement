package com.psybergate.acceptance.drivers.ui.pages;

import com.microsoft.playwright.Page;
import com.psybergate.acceptance.config.EnvironmentConfig;
import org.springframework.stereotype.Component;

import io.cucumber.spring.ScenarioScope;

@Component
@ScenarioScope
public class LoginPage extends BasePage {

	public LoginPage(Page page, EnvironmentConfig env) {
		super(page, env);
	}

	public void open() {
		navigateTo("/login");
	}

	public void fillEmail(String email) {
		page.getByTestId("login-email-input").fill(email);
	}

	public void fillPassword(String password) {
		page.getByTestId("login-password-input").fill(password);
	}

	public void submit() {
		page.getByTestId("login-submit-button").click();
	}

	public String getErrorMessage() {
		return page.getByTestId("login-error-message").textContent();
	}

	public boolean isOnLoginPage() {
		return page.url().contains("/login");
	}

	public String getCurrentUrl() {
		return page.url();
	}

	public void reload() {
		page.reload();
	}
}
