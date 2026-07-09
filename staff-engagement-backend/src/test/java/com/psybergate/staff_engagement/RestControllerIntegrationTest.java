package com.psybergate.staff_engagement;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests verifying each REST endpoint returns seed data
 * with expected minimum record counts and correct JSON structure.
 *
 * Validates: Requirements 9.1, 9.2, 9.3, 9.4, 9.5, 9.6
 */
class RestControllerIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void getUsersReturnsAtLeastThreeRecordsWithExpectedFields() {
		ResponseEntity<String> response = restTemplate.getForEntity("/api/users", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		String body = response.getBody();
		assertThat(body).isNotNull();

		List<?> users = JsonPath.read(body, "$");
		assertThat(users).hasSizeGreaterThanOrEqualTo(3);

		// Verify JSON structure contains expected fields
		assertThat((String) JsonPath.read(body, "$[0].name")).isNotBlank();
		assertThat((String) JsonPath.read(body, "$[0].email")).contains("@");
		assertThat((Object) JsonPath.read(body, "$[0].id")).isNotNull();
		assertThat((Object) JsonPath.read(body, "$[0].createdAt")).isNotNull();
	}

	@Test
	void getEmployeesReturnsAtLeastFiveRecordsWithExpectedFields() {
		ResponseEntity<String> response = restTemplate.getForEntity("/api/employees", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		String body = response.getBody();
		assertThat(body).isNotNull();

		List<?> employees = JsonPath.read(body, "$");
		assertThat(employees).hasSizeGreaterThanOrEqualTo(5);

		// Verify JSON structure contains expected fields
		assertThat((String) JsonPath.read(body, "$[0].name")).isNotBlank();
		assertThat((String) JsonPath.read(body, "$[0].email")).contains("@");
		assertThat((Object) JsonPath.read(body, "$[0].id")).isNotNull();
		assertThat((Object) JsonPath.read(body, "$[0].createdAt")).isNotNull();
	}

	@Test
	void getCompaniesReturnsAtLeastTwoRecordsWithExpectedFields() {
		ResponseEntity<String> response = restTemplate.getForEntity("/api/companies", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		String body = response.getBody();
		assertThat(body).isNotNull();

		List<?> companies = JsonPath.read(body, "$");
		assertThat(companies).hasSizeGreaterThanOrEqualTo(2);

		// Verify JSON structure contains expected fields
		assertThat((String) JsonPath.read(body, "$[0].name")).isNotBlank();
		assertThat((Object) JsonPath.read(body, "$[0].id")).isNotNull();
		assertThat((Object) JsonPath.read(body, "$[0].createdAt")).isNotNull();
	}

	@Test
	void getProjectsReturnsAtLeastThreeRecordsWithExpectedFields() {
		ResponseEntity<String> response = restTemplate.getForEntity("/api/projects", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		String body = response.getBody();
		assertThat(body).isNotNull();

		List<?> projects = JsonPath.read(body, "$");
		assertThat(projects).hasSizeGreaterThanOrEqualTo(3);

		// Verify JSON structure contains expected fields
		assertThat((String) JsonPath.read(body, "$[0].name")).isNotBlank();
		assertThat((Object) JsonPath.read(body, "$[0].id")).isNotNull();
		assertThat((Object) JsonPath.read(body, "$[0].createdAt")).isNotNull();
	}

	@Test
	void getInteractionsReturnsAtLeastThreeRecordsWithAtLeastTwoDistinctTypes() {
		ResponseEntity<String> response = restTemplate.getForEntity("/api/interactions", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		String body = response.getBody();
		assertThat(body).isNotNull();

		List<?> interactions = JsonPath.read(body, "$");
		assertThat(interactions).hasSizeGreaterThanOrEqualTo(3);

		// Verify JSON structure contains expected fields
		assertThat((Object) JsonPath.read(body, "$[0].id")).isNotNull();
		assertThat((String) JsonPath.read(body, "$[0].type")).isNotBlank();
		assertThat((String) JsonPath.read(body, "$[0].notes")).isNotBlank();
		assertThat((Object) JsonPath.read(body, "$[0].occurredAt")).isNotNull();
		assertThat((Object) JsonPath.read(body, "$[0].createdAt")).isNotNull();

		// Verify at least 2 distinct interaction types
		List<String> types = JsonPath.read(body, "$[*].type");
		Set<String> distinctTypes = types.stream().collect(Collectors.toSet());
		assertThat(distinctTypes).hasSizeGreaterThanOrEqualTo(2);
	}

	@Test
	void getTasksReturnsAtLeastThreeRecordsWithAtLeastOneOpenAndOneDone() {
		ResponseEntity<String> response = restTemplate.getForEntity("/api/tasks", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		String body = response.getBody();
		assertThat(body).isNotNull();

		List<?> tasks = JsonPath.read(body, "$");
		assertThat(tasks).hasSizeGreaterThanOrEqualTo(3);

		// Verify JSON structure contains expected fields
		assertThat((Object) JsonPath.read(body, "$[0].id")).isNotNull();
		assertThat((String) JsonPath.read(body, "$[0].title")).isNotBlank();
		assertThat((String) JsonPath.read(body, "$[0].status")).isNotBlank();
		assertThat((Object) JsonPath.read(body, "$[0].createdAt")).isNotNull();

		// Verify at least one OPEN and one DONE status
		List<String> statuses = JsonPath.read(body, "$[*].status");
		assertThat(statuses).contains("OPEN");
		assertThat(statuses).contains("DONE");
	}
}
