package com.psybergate.acceptance.drivers.ui.pages;

import com.microsoft.playwright.Page;
import com.psybergate.acceptance.config.EnvironmentConfig;
import org.springframework.stereotype.Component;

import io.cucumber.spring.ScenarioScope;

@Component
@ScenarioScope
public class DashboardPage extends BasePage {

	public DashboardPage(Page page, EnvironmentConfig env) {
		super(page, env);
	}

	public boolean isDashboardVisible() {
		return page.getByTestId("dashboard").isVisible();
	}

	public void waitForDashboard() {
		waitForVisible("[data-testid=\"dashboard\"]");
	}

	public boolean isSkeletonCardVisible() {
		return page.getByTestId("skeleton-card").isVisible();
	}
}
