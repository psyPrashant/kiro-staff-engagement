package com.psybergate.acceptance.hooks;

import com.microsoft.playwright.Page;
import io.cucumber.java.After;
import io.cucumber.java.Scenario;
import org.springframework.stereotype.Component;

@Component
public class ScreenshotHooks {

	private final Page page;

	public ScreenshotHooks(Page page) {
		this.page = page;
	}

	@After
	public void screenshotOnFailure(Scenario scenario) {
		if (scenario.isFailed()) {
			byte[] screenshot = page.screenshot(
				new Page.ScreenshotOptions().setFullPage(true)
			);
			scenario.attach(screenshot, "image/png", "failure-screenshot");
		}
	}
}
