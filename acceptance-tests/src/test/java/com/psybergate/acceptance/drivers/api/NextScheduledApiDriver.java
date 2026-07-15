package com.psybergate.acceptance.drivers.api;

import com.psybergate.acceptance.config.EnvironmentConfig;
import org.springframework.stereotype.Component;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;

@Component
public class NextScheduledApiDriver extends BaseApiDriver {

	public NextScheduledApiDriver(EnvironmentConfig env, HttpClient httpClient) {
		super(env, httpClient);
	}

	/**
	 * Schedule a future interaction for an employee.
	 * POST /api/scheduled-interactions
	 */
	public HttpResponse<String> scheduleInteraction(String body) {
		return post("/api/scheduled-interactions", body);
	}

	/**
	 * Get the Employee 360 response for a given employee.
	 * GET /api/employees/{id}/360
	 */
	public HttpResponse<String> getEmployee360(Long employeeId) {
		return get("/api/employees/" + employeeId + "/360");
	}

	/**
	 * Get the employees list.
	 * GET /api/employees
	 */
	public HttpResponse<String> getEmployees() {
		return get("/api/employees");
	}
}
