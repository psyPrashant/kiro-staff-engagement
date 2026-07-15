package com.psybergate.acceptance.drivers.api;

import com.psybergate.acceptance.config.EnvironmentConfig;
import org.springframework.stereotype.Component;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;

/**
 * Authenticates against the backend's session-cookie login. Because every API driver shares the
 * same cookie-aware {@link HttpClient}, a successful login here establishes the JSESSIONID session
 * used by all subsequent API calls in the scenario.
 */
@Component
public class AuthApiDriver extends BaseApiDriver {

	public AuthApiDriver(EnvironmentConfig env, HttpClient httpClient) {
		super(env, httpClient);
	}

	public HttpResponse<String> login(String email, String password) {
		String body = "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
		return post("/api/auth/login", body);
	}
}
