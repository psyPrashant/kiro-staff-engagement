package com.psybergate.staff_engagement.task;

import com.jayway.jsonpath.JsonPath;
import com.psybergate.staff_engagement.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Task creation via POST /api/tasks.
 *
 * Validates: Requirements 1.5, 2.1, 2.4, 2.6, 3.2, 4.1, 5.1, 5.3
 */
class TaskIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private TestRestTemplate restTemplate;

	private String sessionCookie;

	@BeforeEach
	void authenticateAndGetCookie() {
		HttpHeaders loginHeaders = new HttpHeaders();
		loginHeaders.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> loginRequest = new HttpEntity<>(
				"{\"email\":\"alice.johnson@psybergate.com\",\"password\":\"Password1\"}", loginHeaders);
		ResponseEntity<String> loginResponse = restTemplate.postForEntity("/api/auth/login", loginRequest, String.class);
		assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		List<String> cookies = loginResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
		assertThat(cookies).isNotNull().isNotEmpty();
		sessionCookie = cookies.stream()
				.filter(c -> c.startsWith("JSESSIONID"))
				.findFirst()
				.orElseThrow();
	}

	private ResponseEntity<String> getWithAuth(String url) {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.COOKIE, sessionCookie);
		return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
	}

	private ResponseEntity<String> postWithAuth(String url, String jsonBody) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add(HttpHeaders.COOKIE, sessionCookie);
		return restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(jsonBody, headers), String.class);
	}

	private ResponseEntity<String> putWithAuth(String url, String jsonBody) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add(HttpHeaders.COOKIE, sessionCookie);
		return restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(jsonBody, headers), String.class);
	}

	private ResponseEntity<String> deleteWithAuth(String url) {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.COOKIE, sessionCookie);
		return restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
	}

	@Test
	void createTask_validRequest_returns201WithStatusOpen() {
		String requestBody = """
				{
					"title": "Integration test task"
				}
				""";

		ResponseEntity<String> response = postWithAuth("/api/tasks", requestBody);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		String body = response.getBody();
		assertThat(body).isNotNull();

		assertThat((Object) JsonPath.read(body, "$.id")).isNotNull();
		assertThat((String) JsonPath.read(body, "$.title")).isEqualTo("Integration test task");
		assertThat((String) JsonPath.read(body, "$.status")).isEqualTo("OPEN");
	}

	@Test
	void createTask_linkedToInteraction_returns201() {
		// First create an interaction to link the task to
		ResponseEntity<String> employeesResponse = getWithAuth("/api/employees");
		assertThat(employeesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		Integer employeeId = JsonPath.read(employeesResponse.getBody(), "$[0].id");

		ResponseEntity<String> usersResponse = getWithAuth("/api/users");
		assertThat(usersResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		Integer conductedByUserId = JsonPath.read(usersResponse.getBody(), "$[0].id");
		Integer loggedByUserId = JsonPath.read(usersResponse.getBody(), "$[1].id");

		String interactionBody = """
				{
					"employeeId": %d,
					"conductedByUserId": %d,
					"loggedByUserId": %d,
					"type": "CHECK_IN",
					"notes": "Interaction for task link test",
					"occurredAt": "2024-12-01T10:00:00Z"
				}
				""".formatted(employeeId, conductedByUserId, loggedByUserId);

		ResponseEntity<String> interactionResponse = postWithAuth("/api/interactions", interactionBody);
		assertThat(interactionResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		Integer interactionId = JsonPath.read(interactionResponse.getBody(), "$.id");

		// Now create a task linked to that interaction
		String taskBody = """
				{
					"title": "Task linked to interaction",
					"interactionId": %d
				}
				""".formatted(interactionId);

		ResponseEntity<String> response = postWithAuth("/api/tasks", taskBody);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		String body = response.getBody();
		assertThat(body).isNotNull();

		assertThat((Object) JsonPath.read(body, "$.id")).isNotNull();
		assertThat((Integer) JsonPath.read(body, "$.interactionId")).isEqualTo(interactionId);
	}

	@Test
	void createTask_withEmployeeId_returns201WithEmployeeFields() {
		// Get an employee to link
		ResponseEntity<String> employeesResponse = getWithAuth("/api/employees");
		assertThat(employeesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		Integer employeeId = JsonPath.read(employeesResponse.getBody(), "$[0].id");
		String employeeName = JsonPath.read(employeesResponse.getBody(), "$[0].name");

		// Create a task linked directly to the employee
		String taskBody = """
				{
					"title": "Task linked to employee",
					"employeeId": %d
				}
				""".formatted(employeeId);

		ResponseEntity<String> response = postWithAuth("/api/tasks", taskBody);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		String body = response.getBody();
		assertThat(body).isNotNull();

		assertThat((Object) JsonPath.read(body, "$.id")).isNotNull();
		assertThat((Integer) JsonPath.read(body, "$.employeeId")).isEqualTo(employeeId);
		assertThat((String) JsonPath.read(body, "$.employeeName")).isEqualTo(employeeName);
		assertThat((String) JsonPath.read(body, "$.status")).isEqualTo("OPEN");
	}

	@Test
	void getTasks_taskWithoutEmployeeId_hasNullEmployeeFields() {
		// Create a task without employeeId
		String taskBody = """
				{
					"title": "Task without employee"
				}
				""";

		ResponseEntity<String> createResponse = postWithAuth("/api/tasks", taskBody);
		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		Integer createdTaskId = JsonPath.read(createResponse.getBody(), "$.id");

		// GET all tasks and find the one we just created
		ResponseEntity<String> getResponse = getWithAuth("/api/tasks");
		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		String body = getResponse.getBody();
		assertThat(body).isNotNull();

		List<Integer> ids = JsonPath.read(body, "$[*].id");
		int taskIndex = ids.indexOf(createdTaskId);
		assertThat(taskIndex).isGreaterThanOrEqualTo(0);

		Object employeeId = JsonPath.read(body, "$[" + taskIndex + "].employeeId");
		Object employeeName = JsonPath.read(body, "$[" + taskIndex + "].employeeName");
		assertThat(employeeId).isNull();
		assertThat(employeeName).isNull();
	}

	@Test
	void createTask_nonExistentInteractionId_returns400() {
		String requestBody = """
				{
					"title": "Task with bad interaction",
					"interactionId": 99999
				}
				""";

		ResponseEntity<String> response = postWithAuth("/api/tasks", requestBody);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		String body = response.getBody();
		assertThat(body).isNotNull();
		assertThat((String) JsonPath.read(body, "$.message")).contains("Interaction not found");
	}

	// --- PUT /api/tasks/{id} ---

	@Test
	void updateTask_validRequest_updatesAndPersistsFields() {
		// Create a task
		String createBody = """
				{
					"title": "Original title",
					"description": "Original description"
				}
				""";
		ResponseEntity<String> createResponse = postWithAuth("/api/tasks", createBody);
		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		Integer taskId = JsonPath.read(createResponse.getBody(), "$.id");

		// Update title/description/dueDate/status
		String updateBody = """
				{
					"title": "Updated title",
					"description": "Updated description",
					"dueDate": "2025-09-30",
					"status": "DONE"
				}
				""";
		ResponseEntity<String> updateResponse = putWithAuth("/api/tasks/" + taskId, updateBody);

		assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		String updateBodyResp = updateResponse.getBody();
		assertThat(updateBodyResp).isNotNull();
		assertThat((String) JsonPath.read(updateBodyResp, "$.title")).isEqualTo("Updated title");
		assertThat((String) JsonPath.read(updateBodyResp, "$.description")).isEqualTo("Updated description");
		assertThat((String) JsonPath.read(updateBodyResp, "$.dueDate")).isEqualTo("2025-09-30");
		assertThat((String) JsonPath.read(updateBodyResp, "$.status")).isEqualTo("DONE");

		// GET all tasks and verify persisted state
		ResponseEntity<String> getResponse = getWithAuth("/api/tasks");
		String getBody = getResponse.getBody();
		List<Integer> ids = JsonPath.read(getBody, "$[*].id");
		int idx = ids.indexOf(taskId);
		assertThat(idx).isGreaterThanOrEqualTo(0);
		assertThat((String) JsonPath.read(getBody, "$[" + idx + "].title")).isEqualTo("Updated title");
		assertThat((String) JsonPath.read(getBody, "$[" + idx + "].status")).isEqualTo("DONE");
	}

	@Test
	void updateTask_nullEmployeeId_clearsEmployeeAssociation() {
		// Get an employee and create a task linked to it
		ResponseEntity<String> employeesResponse = getWithAuth("/api/employees");
		Integer employeeId = JsonPath.read(employeesResponse.getBody(), "$[0].id");

		String createBody = """
				{
					"title": "Task with employee",
					"employeeId": %d
				}
				""".formatted(employeeId);
		ResponseEntity<String> createResponse = postWithAuth("/api/tasks", createBody);
		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		Integer taskId = JsonPath.read(createResponse.getBody(), "$.id");
		assertThat((Integer) JsonPath.read(createResponse.getBody(), "$.employeeId")).isEqualTo(employeeId);

		// Update without employeeId -> association cleared
		String updateBody = """
				{
					"title": "Task with employee"
				}
				""";
		ResponseEntity<String> updateResponse = putWithAuth("/api/tasks/" + taskId, updateBody);
		assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat((Object) JsonPath.read(updateResponse.getBody(), "$.employeeId")).isNull();
		assertThat((Object) JsonPath.read(updateResponse.getBody(), "$.employeeName")).isNull();

		// GET verifies the cleared association is persisted
		ResponseEntity<String> getResponse = getWithAuth("/api/tasks");
		String getBody = getResponse.getBody();
		List<Integer> ids = JsonPath.read(getBody, "$[*].id");
		int idx = ids.indexOf(taskId);
		assertThat((Object) JsonPath.read(getBody, "$[" + idx + "].employeeId")).isNull();
	}

	@Test
	void updateTask_nonExistentId_returns404() {
		String updateBody = """
				{
					"title": "Does not matter"
				}
				""";
		ResponseEntity<String> response = putWithAuth("/api/tasks/999999", updateBody);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat((String) JsonPath.read(response.getBody(), "$.message")).contains("Task not found");
	}

	@Test
	void updateTask_blankTitle_returns400() {
		String createBody = """
				{
					"title": "A task to update"
				}
				""";
		ResponseEntity<String> createResponse = postWithAuth("/api/tasks", createBody);
		Integer taskId = JsonPath.read(createResponse.getBody(), "$.id");

		String updateBody = """
				{
					"title": ""
				}
				""";
		ResponseEntity<String> response = putWithAuth("/api/tasks/" + taskId, updateBody);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat((Object) JsonPath.read(response.getBody(), "$.fieldErrors.title")).isNotNull();
	}

	// --- DELETE /api/tasks/{id} ---

	@Test
	void deleteTask_existingTask_removesTask() {
		String createBody = """
				{
					"title": "Task to delete"
				}
				""";
		ResponseEntity<String> createResponse = postWithAuth("/api/tasks", createBody);
		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		Integer taskId = JsonPath.read(createResponse.getBody(), "$.id");

		ResponseEntity<String> deleteResponse = deleteWithAuth("/api/tasks/" + taskId);
		assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		// GET no longer includes the deleted task
		ResponseEntity<String> getResponse = getWithAuth("/api/tasks");
		List<Integer> ids = JsonPath.read(getResponse.getBody(), "$[*].id");
		assertThat(ids).doesNotContain(taskId);
	}

	@Test
	void deleteTask_nonExistentId_returns404() {
		ResponseEntity<String> response = deleteWithAuth("/api/tasks/999999");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat((String) JsonPath.read(response.getBody(), "$.message")).contains("Task not found");
	}
}
