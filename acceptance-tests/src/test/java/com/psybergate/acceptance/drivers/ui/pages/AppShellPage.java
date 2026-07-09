package com.psybergate.acceptance.drivers.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.psybergate.acceptance.config.EnvironmentConfig;
import org.springframework.stereotype.Component;

import io.cucumber.spring.ScenarioScope;

import java.util.List;

@Component
@ScenarioScope
public class AppShellPage extends BasePage {

	private static final String NAV_BAR = "[data-testid=\"app-nav\"]";
	private static final String NAV_LINK_USER = "[data-testid=\"nav-link-user\"]";
	private static final String NAV_LINK_EMPLOYEE = "[data-testid=\"nav-link-employee\"]";
	private static final String NAV_LINK_CLIENT = "[data-testid=\"nav-link-client\"]";
	private static final String NAV_LINK_INTERACTION = "[data-testid=\"nav-link-interaction\"]";
	private static final String NAV_LINK_TASK = "[data-testid=\"nav-link-task\"]";
	private static final String LOGOUT_BUTTON = "[data-testid=\"logout-button\"]";

	public AppShellPage(Page page, EnvironmentConfig env) {
		super(page, env);
	}

	public boolean isNavBarVisible() {
		return isVisible(NAV_BAR);
	}

	public List<String> getNavLinkTexts() {
		Locator links = locator(NAV_BAR + " a");
		return links.allTextContents();
	}

	public boolean isNavLinkVisible(String testId) {
		return isVisible("[data-testid=\"" + testId + "\"]");
	}
}
