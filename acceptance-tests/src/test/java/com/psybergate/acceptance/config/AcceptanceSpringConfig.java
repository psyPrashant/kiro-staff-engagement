package com.psybergate.acceptance.config;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.cucumber.spring.CucumberContextConfiguration;
import io.cucumber.spring.ScenarioScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@CucumberContextConfiguration
@ComponentScan(basePackages = "com.psybergate.acceptance")
public class AcceptanceSpringConfig {

	@Value("${playwright.headless:true}")
	private boolean headless;

	@Bean
	@ScenarioScope
	public Browser browser() {
		return Playwright.create().chromium().launch(
			new BrowserType.LaunchOptions().setHeadless(headless)
		);
	}

	@Bean
	@ScenarioScope
	public BrowserContext browserContext(Browser browser) {
		return browser.newContext();
	}

	@Bean
	@ScenarioScope
	public Page page(BrowserContext context) {
		return context.newPage();
	}
}
