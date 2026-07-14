package com.psybergate.acceptance.domain.scheduling;

import com.psybergate.acceptance.drivers.api.NextScheduledApiDriver;
import com.psybergate.acceptance.world.TestWorld;
import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;

import java.net.http.HttpResponse;

@Component
@ScenarioScope
public class NextScheduledActor {

	private final NextScheduledApiDriver apiDriver;
	private final TestWorld testWorld;

	public NextScheduledActor(NextScheduledApiDriver apiDriver, TestWorld testWorld) {
		this.apiDriver = apiDriver;
		this.testWorld = testWorld;
	}

	/**
	 * Schedule a future interaction for an employee via the API.
	 */
	public void scheduleInteractionForEmployee(Long employeeId, String date, String type) {
		String body = """
			{
			  "employeeId": %d,
			  "scheduledDate": "%s",
			  "interactionType": "%s"
			}
			""".formatted(employeeId, date, type);
		HttpResponse<String> response = apiDriver.scheduleInteraction(body);
		testWorld.set("lastScheduleResponse", response);
	}

	/**
	 * Fetch the Employee 360 response for a given employee.
	 */
	public HttpResponse<String> fetchEmployee360(Long employeeId) {
		HttpResponse<String> response = apiDriver.getEmployee360(employeeId);
		testWorld.set("lastEmployee360Response", response);
		return response;
	}

	/**
	 * Fetch the employees list.
	 */
	public HttpResponse<String> fetchEmployeesList() {
		HttpResponse<String> response = apiDriver.getEmployees();
		testWorld.set("lastEmployeesListResponse", response);
		return response;
	}
}
