package com.psybergate.acceptance.hooks;

import com.microsoft.playwright.Page;
import io.cucumber.java.After;
import io.cucumber.java.Scenario;

public class ScreenshotHooks {

	private final Page page;

	public ScreenshotHooks(Page page) {
		this.page = page;
	}

	// Only UI scenarios need a browser screenshot; @backend-seed (API-only) scenarios skip this
	// so they never trigger a Playwright browser launch.
	@After("not @backend-seed")
	public void screenshotOnFailure(Scenario scenario) {
		if (scenario.isFailed()) {
			byte[] screenshot = page.screenshot(
				new Page.ScreenshotOptions().setFullPage(true)
			);
			scenario.attach(screenshot, "image/png", "failure-screenshot");
		}
	}
}
