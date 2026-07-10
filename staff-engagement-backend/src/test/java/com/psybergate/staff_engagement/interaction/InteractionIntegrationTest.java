package com.psybergate.staff_engagement.interaction;

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
 * Integration tests for Interaction creation via POST /api/interactions.
 *
 * Validates: Requirements 1.1, 1.4
 */
class InteractionIntegrationTest extends BaseIntegrationTest {

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

	@Test
	void createInteraction_validRequest_returns201AndPersists() {
		// Get a real employee ID from seed data
		ResponseEntity<String> employeesResponse = getWithAuth("/api/employees");
		assertThat(employeesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		Integer employeeId = JsonPath.read(employeesResponse.getBody(), "$[0].id");

		// Get real user IDs from seed data
		ResponseEntity<String> usersResponse = getWithAuth("/api/users");
		assertThat(usersResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		Integer conductedByUserId = JsonPath.read(usersResponse.getBody(), "$[0].id");
		Integer loggedByUserId = JsonPath.read(usersResponse.getBody(), "$[1].id");

		String requestBody = """
				{
					"employeeId": %d,
					"conductedByUserId": %d,
					"loggedByUserId": %d,
					"type": "CHECK_IN",
					"notes": "Integration test interaction",
					"occurredAt": "2024-12-01T10:00:00Z"
				}
				""".formatted(employeeId, conductedByUserId, loggedByUserId);

		ResponseEntity<String> response = postWithAuth("/api/interactions", requestBody);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		String body = response.getBody();
		assertThat(body).isNotNull();

		// Verify response body has expected fields
		assertThat((Object) JsonPath.read(body, "$.id")).isNotNull();
		assertThat((String) JsonPath.read(body, "$.type")).isEqualTo("CHECK_IN");
		assertThat((String) JsonPath.read(body, "$.notes")).isEqualTo("Integration test interaction");
		assertThat((String) JsonPath.read(body, "$.occurredAt")).isEqualTo("2024-12-01T10:00:00Z");
		assertThat((Object) JsonPath.read(body, "$.employee.id")).isNotNull();
		assertThat((Object) JsonPath.read(body, "$.conductedBy.id")).isNotNull();
		assertThat((Object) JsonPath.read(body, "$.loggedBy.id")).isNotNull();
	}

	@Test
	void createInteraction_nonExistentEmployee_returns400() {
		// Get real user IDs from seed data
		ResponseEntity<String> usersResponse = getWithAuth("/api/users");
		assertThat(usersResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		Integer conductedByUserId = JsonPath.read(usersResponse.getBody(), "$[0].id");
		Integer loggedByUserId = JsonPath.read(usersResponse.getBody(), "$[1].id");

		String requestBody = """
				{
					"employeeId": 99999,
					"conductedByUserId": %d,
					"loggedByUserId": %d,
					"type": "CHECK_IN",
					"notes": "Should fail due to non-existent employee",
					"occurredAt": "2024-12-01T10:00:00Z"
				}
				""".formatted(conductedByUserId, loggedByUserId);

		ResponseEntity<String> response = postWithAuth("/api/interactions", requestBody);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		String body = response.getBody();
		assertThat(body).isNotNull();
		assertThat((String) JsonPath.read(body, "$.message")).contains("Employee not found");
	}
}
