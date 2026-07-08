package com.psybergate.acceptance.drivers.ui.pages;

import com.microsoft.playwright.Page;
import com.psybergate.acceptance.config.EnvironmentConfig;
import org.springframework.stereotype.Component;

import io.cucumber.spring.ScenarioScope;

@Component
@ScenarioScope
public class HomePage extends BasePage {

	public HomePage(Page page, EnvironmentConfig env) {
		super(page, env);
	}

	public void open() {
		navigateTo("/");
	}

	public boolean isLoaded() {
		return isVisible("body");
	}
}
