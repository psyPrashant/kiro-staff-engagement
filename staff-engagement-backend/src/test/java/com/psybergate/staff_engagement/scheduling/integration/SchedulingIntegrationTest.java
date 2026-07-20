package com.psybergate.staff_engagement.scheduling.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.psybergate.staff_engagement.employee.domain.Employee;
import com.psybergate.staff_engagement.employee.domain.EmployeeRepository;
import com.psybergate.staff_engagement.scheduling.domain.CompletionStatus;
import com.psybergate.staff_engagement.scheduling.domain.ScheduledInteraction;
import com.psybergate.staff_engagement.scheduling.domain.ScheduledInteractionRepository;
import com.psybergate.staff_engagement.support.BaseIntegrationTest;
import com.psybergate.staff_engagement.user.domain.User;
import com.psybergate.staff_engagement.user.domain.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

/**
 * Integration tests for the Scheduling REST endpoints.
 *
 * Validates: Requirements 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7
 */
class SchedulingIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private EmployeeRepository employeeRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ScheduledInteractionRepository scheduledInteractionRepository;

	private String sessionCookie;
	private Long employeeId;
	private Long userId;

	@BeforeEach
	void authenticateAndSetup() {
		// Clean up scheduled interactions from previous tests
		scheduledInteractionRepository.deleteAll();

		// Authenticate
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

		// Get a real employee ID from seed data
		Employee employee = employeeRepository.findAll().stream().findFirst().orElseThrow();
		employeeId = employee.getId();

		// Get the authenticated user's ID
		User user = userRepository.findByEmail("alice.johnson@psybergate.com").orElseThrow();
		userId = user.getId();
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

	private ResponseEntity<String> patchWithAuth(String url, String jsonBody) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add(HttpHeaders.COOKIE, sessionCookie);
		return restTemplate.exchange(url, HttpMethod.PATCH, new HttpEntity<>(jsonBody, headers), String.class);
	}

	@Test
	void createScheduledInteraction_validRequest_returns201WithExpectedFields() {
		LocalDate futureDate = LocalDate.now().plusDays(7);
		String requestBody = """
				{
					"employeeId": %d,
					"scheduledDate": "%s",
					"interactionType": "CHECK_IN",
					"notes": "Integration test scheduled interaction"
				}
				""".formatted(employeeId, futureDate);

		ResponseEntity<String> response = postWithAuth("/api/scheduled-interactions", requestBody);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		String body = response.getBody();
		assertThat(body).isNotNull();

		assertThat((Object) JsonPath.read(body, "$.id")).isNotNull();
		assertThat(((Number) JsonPath.read(body, "$.employeeId")).longValue()).isEqualTo(employeeId);
		assertThat((String) JsonPath.read(body, "$.scheduledDate")).isEqualTo(futureDate.toString());
		assertThat((String) JsonPath.read(body, "$.interactionType")).isEqualTo("CHECK_IN");
		assertThat((String) JsonPath.read(body, "$.completionStatus")).isEqualTo("PENDING");
		assertThat((Object) JsonPath.read(body, "$.createdAt")).isNotNull();
	}

	@Test
	void listScheduledInteractions_multipleEntries_returnsSortedByDateAscending() {
		LocalDate laterDate = LocalDate.now().plusDays(14);
		LocalDate earlierDate = LocalDate.now().plusDays(3);

		// Insert in reverse order (later date first) to verify sorting
		String requestLater = """
				{
					"employeeId": %d,
					"scheduledDate": "%s",
					"interactionType": "MENTORING"
				}
				""".formatted(employeeId, laterDate);
		postWithAuth("/api/scheduled-interactions", requestLater);

		String requestEarlier = """
				{
					"employeeId": %d,
					"scheduledDate": "%s",
					"interactionType": "CHECK_IN"
				}
				""".formatted(employeeId, earlierDate);
		postWithAuth("/api/scheduled-interactions", requestEarlier);

		ResponseEntity<String> response = getWithAuth("/api/scheduled-interactions");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		String body = response.getBody();
		assertThat(body).isNotNull();

		List<?> entries = JsonPath.read(body, "$");
		assertThat(entries).hasSizeGreaterThanOrEqualTo(2);

		// Verify ascending order
		List<String> dates = JsonPath.read(body, "$[*].scheduledDate");
		for (int i = 0; i < dates.size() - 1; i++) {
			assertThat(dates.get(i)).isLessThanOrEqualTo(dates.get(i + 1));
		}
	}

	@Test
	void updateScheduledInteraction_pendingToCompleted_returns200WithUpdatedStatus() {
		// Create a PENDING interaction first
		LocalDate futureDate = LocalDate.now().plusDays(5);
		String createRequest = """
				{
					"employeeId": %d,
					"scheduledDate": "%s",
					"interactionType": "CATCH_UP"
				}
				""".formatted(employeeId, futureDate);

		ResponseEntity<String> createResponse = postWithAuth("/api/scheduled-interactions", createRequest);
		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		Integer createdId = JsonPath.read(createResponse.getBody(), "$.id");

		// Transition to COMPLETED
		String updateRequest = """
				{
					"completionStatus": "COMPLETED"
				}
				""";

		ResponseEntity<String> updateResponse = patchWithAuth(
				"/api/scheduled-interactions/" + createdId, updateRequest);

		assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		String body = updateResponse.getBody();
		assertThat(body).isNotNull();

		assertThat((String) JsonPath.read(body, "$.completionStatus")).isEqualTo("COMPLETED");
		assertThat(((Number) JsonPath.read(body, "$.id")).intValue()).isEqualTo(createdId);
	}

	@Test
	void listScheduledInteractions_overdueTrue_returnsOnlyOverduePendingItems() {
		// Insert records directly into the DB with past dates to bypass date validation
		Employee employee = employeeRepository.findById(employeeId).orElseThrow();
		User user = userRepository.findById(userId).orElseThrow();

		// Overdue: past date + PENDING
		ScheduledInteraction overdue = new ScheduledInteraction();
		overdue.setEmployee(employee);
		overdue.setScheduledBy(user);
		overdue.setScheduledDate(LocalDate.now().minusDays(5));
		overdue.setInteractionType(com.psybergate.staff_engagement.interaction.domain.InteractionType.CHECK_IN);
		overdue.setCompletionStatus(CompletionStatus.PENDING);
		scheduledInteractionRepository.save(overdue);

		// Not overdue: future date + PENDING
		ScheduledInteraction futurePending = new ScheduledInteraction();
		futurePending.setEmployee(employee);
		futurePending.setScheduledBy(user);
		futurePending.setScheduledDate(LocalDate.now().plusDays(5));
		futurePending.setInteractionType(com.psybergate.staff_engagement.interaction.domain.InteractionType.MENTORING);
		futurePending.setCompletionStatus(CompletionStatus.PENDING);
		scheduledInteractionRepository.save(futurePending);

		// Not overdue: past date + COMPLETED
		ScheduledInteraction pastCompleted = new ScheduledInteraction();
		pastCompleted.setEmployee(employee);
		pastCompleted.setScheduledBy(user);
		pastCompleted.setScheduledDate(LocalDate.now().minusDays(3));
		pastCompleted.setInteractionType(com.psybergate.staff_engagement.interaction.domain.InteractionType.CATCH_UP);
		pastCompleted.setCompletionStatus(CompletionStatus.COMPLETED);
		scheduledInteractionRepository.save(pastCompleted);

		ResponseEntity<String> response = getWithAuth("/api/scheduled-interactions?overdue=true");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		String body = response.getBody();
		assertThat(body).isNotNull();

		List<?> entries = JsonPath.read(body, "$");
		assertThat(entries).hasSize(1);

		// Verify it's the overdue one
		assertThat((String) JsonPath.read(body, "$[0].completionStatus")).isEqualTo("PENDING");
		assertThat((Boolean) JsonPath.read(body, "$[0].overdue")).isTrue();

		String scheduledDate = JsonPath.read(body, "$[0].scheduledDate");
		assertThat(LocalDate.parse(scheduledDate)).isBefore(LocalDate.now());
	}

	@Test
	void createScheduledInteraction_nonExistentEmployee_returns404() {
		LocalDate futureDate = LocalDate.now().plusDays(7);
		String requestBody = """
				{
					"employeeId": 99999,
					"scheduledDate": "%s",
					"interactionType": "CHECK_IN"
				}
				""".formatted(futureDate);

		ResponseEntity<String> response = postWithAuth("/api/scheduled-interactions", requestBody);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		String body = response.getBody();
		assertThat(body).isNotNull();
		assertThat((String) JsonPath.read(body, "$.message")).containsIgnoringCase("employee");
	}

	@Test
	void updateScheduledInteraction_completedEntity_returns400() {
		// Create and complete an interaction
		LocalDate futureDate = LocalDate.now().plusDays(5);
		String createRequest = """
				{
					"employeeId": %d,
					"scheduledDate": "%s",
					"interactionType": "OTHER"
				}
				""".formatted(employeeId, futureDate);

		ResponseEntity<String> createResponse = postWithAuth("/api/scheduled-interactions", createRequest);
		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		Integer createdId = JsonPath.read(createResponse.getBody(), "$.id");

		// First, complete it
		String completeRequest = """
				{
					"completionStatus": "COMPLETED"
				}
				""";
		ResponseEntity<String> completeResponse = patchWithAuth(
				"/api/scheduled-interactions/" + createdId, completeRequest);
		assertThat(completeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		// Now try to transition COMPLETED → CANCELLED (should fail)
		String invalidTransition = """
				{
					"completionStatus": "CANCELLED"
				}
				""";
		ResponseEntity<String> response = patchWithAuth(
				"/api/scheduled-interactions/" + createdId, invalidTransition);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		String body = response.getBody();
		assertThat(body).isNotNull();
		assertThat((String) JsonPath.read(body, "$.message")).contains("Cannot transition");
	}

	@Test
	void updateScheduledInteraction_cancelledEntity_returns400() {
		// Create and cancel an interaction
		LocalDate futureDate = LocalDate.now().plusDays(5);
		String createRequest = """
				{
					"employeeId": %d,
					"scheduledDate": "%s",
					"interactionType": "CHECK_IN"
				}
				""".formatted(employeeId, futureDate);

		ResponseEntity<String> createResponse = postWithAuth("/api/scheduled-interactions", createRequest);
		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		Integer createdId = JsonPath.read(createResponse.getBody(), "$.id");

		// First, cancel it
		String cancelRequest = """
				{
					"completionStatus": "CANCELLED"
				}
				""";
		ResponseEntity<String> cancelResponse = patchWithAuth(
				"/api/scheduled-interactions/" + createdId, cancelRequest);
		assertThat(cancelResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		// Now try to transition CANCELLED → COMPLETED (should fail)
		String invalidTransition = """
				{
					"completionStatus": "COMPLETED"
				}
				""";
		ResponseEntity<String> response = patchWithAuth(
				"/api/scheduled-interactions/" + createdId, invalidTransition);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		String body = response.getBody();
		assertThat(body).isNotNull();
		assertThat((String) JsonPath.read(body, "$.message")).contains("Cannot transition");
	}
}
