package com.psybergate.acceptance.drivers.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.psybergate.acceptance.config.EnvironmentConfig;

public abstract class BasePage {

	protected final Page page;
	protected final EnvironmentConfig env;

	protected BasePage(Page page, EnvironmentConfig env) {
		this.page = page;
		this.env = env;
	}

	protected void navigateTo(String path) {
		page.navigate(env.appBaseUrl() + path);
	}

	protected Locator locator(String selector) {
		return page.locator(selector);
	}

	protected void waitForVisible(String selector) {
		page.locator(selector).waitFor(
			new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE)
		);
	}

	protected void click(String selector) {
		page.locator(selector).click();
	}

	protected void fill(String selector, String text) {
		page.locator(selector).fill(text);
	}

	protected String textContent(String selector) {
		return page.locator(selector).textContent();
	}

	protected boolean isVisible(String selector) {
		return page.locator(selector).isVisible();
	}
}
