package com.psybergate.acceptance.config;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.cucumber.spring.ScenarioScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.net.CookieManager;
import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Spring bean definitions for the acceptance suite.
 *
 * <p>This is a normal Spring {@code @Configuration} (component-scanning the drivers, actors, page
 * objects and support beans). It is deliberately kept separate from the Cucumber glue — see
 * {@link CucumberSpringConfiguration}.
 */
@Configuration
@ComponentScan(basePackages = "com.psybergate.acceptance")
public class AcceptanceTestConfig {

	@Value("${playwright.headless:true}")
	private boolean headless;

	/**
	 * A single cookie-aware HTTP client shared by all API drivers, so that a session established by
	 * logging in (JSESSIONID cookie) is reused across every subsequent API call in the run.
	 */
	@Bean
	public HttpClient apiHttpClient() {
		return HttpClient.newBuilder()
			.cookieHandler(new CookieManager())
			.connectTimeout(Duration.ofSeconds(10))
			.build();
	}

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
