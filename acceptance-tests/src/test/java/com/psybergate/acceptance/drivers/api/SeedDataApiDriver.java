package com.psybergate.acceptance.drivers.api;

import com.psybergate.acceptance.config.EnvironmentConfig;
import org.springframework.stereotype.Component;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;

@Component
public class SeedDataApiDriver extends BaseApiDriver {

	public SeedDataApiDriver(EnvironmentConfig env, HttpClient httpClient) {
		super(env, httpClient);
	}

	public HttpResponse<String> getUsers() {
		return get("/api/users");
	}

	public HttpResponse<String> getEmployees() {
		return get("/api/employees");
	}

	public HttpResponse<String> getCompanies() {
		return get("/api/companies");
	}

	public HttpResponse<String> getProjects() {
		return get("/api/projects");
	}

	public HttpResponse<String> getInteractions() {
		return get("/api/interactions");
	}

	public HttpResponse<String> getTasks() {
		return get("/api/tasks");
	}
}
