package com.psybergate.acceptance.domain.scheduling;

import com.jayway.jsonpath.JsonPath;
import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Component
@ScenarioScope
public class NextScheduledAssertions {

	/**
	 * Assert that the Employee 360 response body contains a non-null nextScheduled
	 * field with the expected scheduledAt date and interaction type.
	 */
	public void assertNextScheduledEquals(String responseBody, String expectedDate, String expectedType) {
		String scheduledAt = JsonPath.read(responseBody, "$.nextScheduled.scheduledAt");
		String type = JsonPath.read(responseBody, "$.nextScheduled.type");

		assertThat(scheduledAt)
			.as("Expected nextScheduled.scheduledAt to be '%s'", expectedDate)
			.isEqualTo(expectedDate);
		assertThat(type)
			.as("Expected nextScheduled.type to be '%s'", expectedType)
			.isEqualTo(expectedType);
	}

	/**
	 * Assert that the Employee 360 response body has nextScheduled set to null.
	 */
	public void assertNextScheduledIsNull(String responseBody) {
		Object nextScheduled = JsonPath.read(responseBody, "$.nextScheduled");

		assertThat(nextScheduled)
			.as("Expected nextScheduled to be null")
			.isNull();
	}

	/**
	 * Assert that in the employees list response, a specific employee has the expected
	 * nextScheduled values.
	 */
	public void assertEmployeeListNextScheduled(String responseBody, Long employeeId,
												String expectedDate, String expectedType) {
		List<Map<String, Object>> employees = JsonPath.read(responseBody, "$[?(@.id == " + employeeId + ")]");

		assertThat(employees)
			.as("Expected to find employee with id %d in the list", employeeId)
			.isNotEmpty();

		Map<String, Object> employee = employees.get(0);
		@SuppressWarnings("unchecked")
		Map<String, Object> nextScheduled = (Map<String, Object>) employee.get("nextScheduled");

		assertThat(nextScheduled)
			.as("Expected nextScheduled to be non-null for employee %d", employeeId)
			.isNotNull();
		assertThat(nextScheduled.get("scheduledAt"))
			.as("Expected nextScheduled.scheduledAt to be '%s' for employee %d", expectedDate, employeeId)
			.isEqualTo(expectedDate);
		assertThat(nextScheduled.get("type"))
			.as("Expected nextScheduled.type to be '%s' for employee %d", expectedType, employeeId)
			.isEqualTo(expectedType);
	}

	/**
	 * Assert that in the employees list response, a specific employee has nextScheduled
	 * set to null.
	 */
	public void assertEmployeeListNextScheduledIsNull(String responseBody, Long employeeId) {
		List<Map<String, Object>> employees = JsonPath.read(responseBody, "$[?(@.id == " + employeeId + ")]");

		assertThat(employees)
			.as("Expected to find employee with id %d in the list", employeeId)
			.isNotEmpty();

		Map<String, Object> employee = employees.get(0);
		Object nextScheduled = employee.get("nextScheduled");

		assertThat(nextScheduled)
			.as("Expected nextScheduled to be null for employee %d", employeeId)
			.isNull();
	}
}
