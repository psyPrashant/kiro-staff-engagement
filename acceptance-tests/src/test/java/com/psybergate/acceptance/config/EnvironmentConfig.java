package com.psybergate.acceptance.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentConfig {

	@Value("${app.base-url.ui}")
	private String appBaseUrlUi;

	@Value("${app.base-url.api}")
	private String appBaseUrlApi;

	@Value("${playwright.timeout:30000}")
	private int playwrightTimeout;

	public String appBaseUrl() {
		return appBaseUrlUi;
	}

	public String apiBaseUrl() {
		return appBaseUrlApi;
	}

	public int playwrightTimeout() {
		return playwrightTimeout;
	}
}
