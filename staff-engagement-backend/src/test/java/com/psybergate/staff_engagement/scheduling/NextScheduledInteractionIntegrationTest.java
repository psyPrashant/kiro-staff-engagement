package com.psybergate.staff_engagement.scheduling;

import com.jayway.jsonpath.JsonPath;
import com.psybergate.staff_engagement.BaseIntegrationTest;
import com.psybergate.staff_engagement.employee.Employee;
import com.psybergate.staff_engagement.employee.EmployeeRepository;
import com.psybergate.staff_engagement.interaction.InteractionType;
import com.psybergate.staff_engagement.user.User;
import com.psybergate.staff_engagement.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the next-scheduled-interaction feature.
 * Verifies the 360 response and employees list response include correct
 * nextScheduled data using a real PostgreSQL database via Testcontainers.
 *
 * Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.5, 5.6
 */
class NextScheduledInteractionIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private EmployeeRepository employeeRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ScheduledInteractionRepository scheduledInteractionRepository;

	private String sessionCookie;
	private Employee employee;
	private User user;

	@BeforeEach
	void authenticateAndSetup() {
		// Clean up scheduled interactions from previous tests
		scheduledInteractionRepository.deleteAll();

		// Authenticate as the seed user
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

		// Get test data from seed
		employee = employeeRepository.findAll().stream().findFirst().orElseThrow();
		user = userRepository.findByEmail("alice.johnson@psybergate.com").orElseThrow();
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

	private ScheduledInteraction createInteraction(Employee emp, User scheduledBy,
			LocalDate scheduledDate, InteractionType type, CompletionStatus status) {
		ScheduledInteraction si = new ScheduledInteraction();
		si.setEmployee(emp);
		si.setScheduledBy(scheduledBy);
		si.setScheduledDate(scheduledDate);
		si.setInteractionType(type);
		si.setCompletionStatus(status);
		return scheduledInteractionRepository.save(si);
	}

	@Test
	void employee360_multipleFuturePendingInteractions_returnsSoonestOne() {
		// Seed: two future PENDING interactions with different dates
		LocalDate soonerDate = LocalDate.now().plusDays(3);
		LocalDate laterDate = LocalDate.now().plusDays(10);

		createInteraction(employee, user, laterDate, InteractionType.MENTORING, CompletionStatus.PENDING);
		createInteraction(employee, user, soonerDate, InteractionType.CHECK_IN, CompletionStatus.PENDING);

		ResponseEntity<String> response = getWithAuth("/api/employees/" + employee.getId() + "/360");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		String body = response.getBody();
		assertThat(body).isNotNull();

		// nextScheduled should be the sooner one
		assertThat((String) JsonPath.read(body, "$.nextScheduled.scheduledAt"))
				.isEqualTo(soonerDate.toString());
		assertThat((String) JsonPath.read(body, "$.nextScheduled.type"))
				.isEqualTo("CHECK_IN");
	}

	@Test
	void employee360_onlyPastInteractionsOrCompletedCancelled_nextScheduledIsNull() {
		// Seed: past PENDING, past COMPLETED, future COMPLETED, future CANCELLED
		createInteraction(employee, user, LocalDate.now().minusDays(5),
				InteractionType.CHECK_IN, CompletionStatus.PENDING);
		createInteraction(employee, user, LocalDate.now().minusDays(2),
				InteractionType.MENTORING, CompletionStatus.COMPLETED);
		createInteraction(employee, user, LocalDate.now().plusDays(7),
				InteractionType.CATCH_UP, CompletionStatus.COMPLETED);
		createInteraction(employee, user, LocalDate.now().plusDays(14),
				InteractionType.OTHER, CompletionStatus.CANCELLED);

		ResponseEntity<String> response = getWithAuth("/api/employees/" + employee.getId() + "/360");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		String body = response.getBody();
		assertThat(body).isNotNull();

		assertThat((Object) JsonPath.read(body, "$.nextScheduled")).isNull();
	}

	@Test
	void employee360_noScheduledInteractionsAtAll_nextScheduledIsNull() {
		// No interactions seeded for this employee — repository was cleared in @BeforeEach

		ResponseEntity<String> response = getWithAuth("/api/employees/" + employee.getId() + "/360");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		String body = response.getBody();
		assertThat(body).isNotNull();

		assertThat((Object) JsonPath.read(body, "$.nextScheduled")).isNull();
	}

	@Test
	void employeesList_includesCorrectNextScheduledPerEmployee() {
		// Seed a future PENDING interaction for the test employee
		LocalDate futureDate = LocalDate.now().plusDays(5);
		createInteraction(employee, user, futureDate, InteractionType.MENTORING, CompletionStatus.PENDING);

		ResponseEntity<String> response = getWithAuth("/api/employees");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		String body = response.getBody();
		assertThat(body).isNotNull();

		List<?> employees = JsonPath.read(body, "$");
		assertThat(employees).isNotEmpty();

		// Find our test employee in the list by ID
		List<Integer> ids = JsonPath.read(body, "$[*].id");
		int index = -1;
		for (int i = 0; i < ids.size(); i++) {
			if (ids.get(i).longValue() == employee.getId()) {
				index = i;
				break;
			}
		}
		assertThat(index).isGreaterThanOrEqualTo(0);

		// Verify the test employee has nextScheduled populated
		assertThat((String) JsonPath.read(body, "$[" + index + "].nextScheduled.scheduledAt"))
				.isEqualTo(futureDate.toString());
		assertThat((String) JsonPath.read(body, "$[" + index + "].nextScheduled.type"))
				.isEqualTo("MENTORING");

		// Verify that at least one employee exists without nextScheduled (from seed data
		// that has no future PENDING interactions after clearing)
		// Check if any employee has null nextScheduled
		List<?> allEmployees = JsonPath.read(body, "$");
		boolean hasNullNextScheduled = false;
		for (int i = 0; i < allEmployees.size(); i++) {
			Object nextScheduled = JsonPath.read(body, "$[" + i + "].nextScheduled");
			if (nextScheduled == null) {
				hasNullNextScheduled = true;
				break;
			}
		}
		// Other employees should have null since we only created interaction for one
		if (allEmployees.size() > 1) {
			assertThat(hasNullNextScheduled).isTrue();
		}
	}

	@Test
	void employee360_newEarlierPendingInteraction_updatesNextScheduled() {
		// Seed: one future PENDING interaction
		LocalDate originalDate = LocalDate.now().plusDays(10);
		createInteraction(employee, user, originalDate, InteractionType.MENTORING, CompletionStatus.PENDING);

		// Verify initial state
		ResponseEntity<String> firstResponse = getWithAuth("/api/employees/" + employee.getId() + "/360");
		assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat((String) JsonPath.read(firstResponse.getBody(), "$.nextScheduled.scheduledAt"))
				.isEqualTo(originalDate.toString());
		assertThat((String) JsonPath.read(firstResponse.getBody(), "$.nextScheduled.type"))
				.isEqualTo("MENTORING");

		// Create a new PENDING interaction with an earlier date via the API
		LocalDate earlierDate = LocalDate.now().plusDays(2);
		String requestBody = """
				{
					"employeeId": %d,
					"scheduledDate": "%s",
					"interactionType": "CHECK_IN",
					"notes": "Earlier check-in"
				}
				""".formatted(employee.getId(), earlierDate);

		ResponseEntity<String> createResponse = postWithAuth("/api/scheduled-interactions", requestBody);
		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

		// Verify the 360 response now reflects the newer, earlier interaction
		ResponseEntity<String> updatedResponse = getWithAuth("/api/employees/" + employee.getId() + "/360");
		assertThat(updatedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		String updatedBody = updatedResponse.getBody();
		assertThat(updatedBody).isNotNull();

		assertThat((String) JsonPath.read(updatedBody, "$.nextScheduled.scheduledAt"))
				.isEqualTo(earlierDate.toString());
		assertThat((String) JsonPath.read(updatedBody, "$.nextScheduled.type"))
				.isEqualTo("CHECK_IN");
	}
}
