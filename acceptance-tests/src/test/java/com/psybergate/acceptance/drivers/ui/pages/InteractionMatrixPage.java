package com.psybergate.acceptance.drivers.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.psybergate.acceptance.config.EnvironmentConfig;
import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;

@Component
@ScenarioScope
public class InteractionMatrixPage extends BasePage {

	public InteractionMatrixPage(Page page, EnvironmentConfig env) {
		super(page, env);
	}

	public void open() {
		navigateTo("/dashboard");
	}

	public void waitForMatrixLoaded() {
		waitForVisible("table tbody tr");
	}

	/**
	 * Clicks the "Schedule Next" button in the row containing the specified employee name.
	 * Locates the table row by matching the employee name text, then clicks the
	 * button with data-testid="schedule-next-btn" within that row.
	 */
	public void clickScheduleNext(String employeeName) {
		Locator row = page.locator("tr", new Page.LocatorOptions().setHasText(employeeName));
		row.locator("[data-testid='schedule-next-btn']").click();
	}
}
