package com.psybergate.acceptance.drivers.api;

import com.psybergate.acceptance.config.EnvironmentConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public abstract class BaseApiDriver {

	protected final EnvironmentConfig env;
	protected final HttpClient httpClient;

	/**
	 * @param httpClient the shared, cookie-aware client (see {@code AcceptanceTestConfig#apiHttpClient})
	 *                   so the authenticated session is reused across all drivers.
	 */
	protected BaseApiDriver(EnvironmentConfig env, HttpClient httpClient) {
		this.env = env;
		this.httpClient = httpClient;
	}

	protected HttpResponse<String> get(String path) {
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(env.apiBaseUrl() + path))
			.GET()
			.header("Content-Type", "application/json")
			.build();
		return send(request);
	}

	protected HttpResponse<String> post(String path, String body) {
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(env.apiBaseUrl() + path))
			.POST(HttpRequest.BodyPublishers.ofString(body))
			.header("Content-Type", "application/json")
			.build();
		return send(request);
	}

	protected HttpResponse<String> put(String path, String body) {
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(env.apiBaseUrl() + path))
			.PUT(HttpRequest.BodyPublishers.ofString(body))
			.header("Content-Type", "application/json")
			.build();
		return send(request);
	}

	protected HttpResponse<String> delete(String path) {
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(env.apiBaseUrl() + path))
			.DELETE()
			.header("Content-Type", "application/json")
			.build();
		return send(request);
	}

	private HttpResponse<String> send(HttpRequest request) {
		try {
			return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		} catch (IOException | InterruptedException e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			throw new RuntimeException("HTTP request failed: " + request.uri(), e);
		}
	}
}
