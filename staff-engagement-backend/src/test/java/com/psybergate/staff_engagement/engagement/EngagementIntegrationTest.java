package com.psybergate.staff_engagement.engagement;

import com.psybergate.staff_engagement.BaseIntegrationTest;
import com.psybergate.staff_engagement.employee.Employee;
import com.psybergate.staff_engagement.employee.EmployeeRepository;
import com.psybergate.staff_engagement.interaction.Interaction;
import com.psybergate.staff_engagement.interaction.InteractionRepository;
import com.psybergate.staff_engagement.interaction.InteractionType;
import com.psybergate.staff_engagement.user.User;
import com.psybergate.staff_engagement.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.*;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for the Engagement Matrix endpoint using Testcontainers PostgreSQL.
 * Seeds employees with varying interaction histories and verifies the full response.
 *
 * Validates: Requirements 8.1, 8.2, 8.3, 8.4, 8.5
 */
@AutoConfigureMockMvc
@Import(EngagementIntegrationTest.TestClockConfig.class)
class EngagementIntegrationTest extends BaseIntegrationTest {

	/**
	 * Fixed clock at 2025-01-15 UTC for deterministic recency calculations.
	 */
	@TestConfiguration
	static class TestClockConfig {
		@Bean
		@Primary
		public Clock testClock() {
			return Clock.fixed(
					LocalDate.of(2025, 1, 15).atStartOfDay(ZoneId.of("UTC")).toInstant(),
					ZoneId.of("UTC"));
		}
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EmployeeRepository employeeRepository;

	@Autowired
	private InteractionRepository interactionRepository;

	@Autowired
	private UserRepository userRepository;

	private Employee noInteractionEmployee;
	private Employee recentEmployee;
	private Employee oldEmployee;

	@BeforeEach
	void setUp() {
		// Clean existing data in correct FK order using JDBC for reliability
		jdbcTemplate.execute("DELETE FROM scheduled_interactions");
		jdbcTemplate.execute("DELETE FROM tasks");
		jdbcTemplate.execute("DELETE FROM interactions");
		jdbcTemplate.execute("DELETE FROM employees");

		// Create a user for FK constraints on interactions
		User testUser = userRepository.findAll().stream().findFirst().orElseGet(() -> {
			User u = new User();
			u.setName("Test User");
			u.setEmail("integration-test-user@test.com");
			u.setPasswordHash("$2a$10$dummyhashvalue");
			return userRepository.save(u);
		});

		// Employee with no interactions → OVERDUE
		noInteractionEmployee = new Employee();
		noInteractionEmployee.setName("No Interaction Employee");
		noInteractionEmployee.setEmail("nointeraction@test.com");
		noInteractionEmployee = employeeRepository.save(noInteractionEmployee);

		// Employee with recent interaction (2025-01-10 → 5 days ago from 2025-01-15) → ON_TRACK
		recentEmployee = new Employee();
		recentEmployee.setName("Recent Employee");
		recentEmployee.setEmail("recent@test.com");
		recentEmployee = employeeRepository.save(recentEmployee);

		Interaction recentInteraction = new Interaction();
		recentInteraction.setEmployee(recentEmployee);
		recentInteraction.setConductedBy(testUser);
		recentInteraction.setLoggedBy(testUser);
		recentInteraction.setType(InteractionType.CHECK_IN);
		recentInteraction.setNotes("Recent check-in");
		recentInteraction.setOccurredAt(
				LocalDate.of(2025, 1, 10).atStartOfDay(ZoneId.of("UTC")).toInstant());
		interactionRepository.save(recentInteraction);

		// Employee with old interaction (2024-12-01 → 45 days ago from 2025-01-15) → OVERDUE
		oldEmployee = new Employee();
		oldEmployee.setName("Old Employee");
		oldEmployee.setEmail("old@test.com");
		oldEmployee = employeeRepository.save(oldEmployee);

		Interaction oldInteraction = new Interaction();
		oldInteraction.setEmployee(oldEmployee);
		oldInteraction.setConductedBy(testUser);
		oldInteraction.setLoggedBy(testUser);
		oldInteraction.setType(InteractionType.MENTORING);
		oldInteraction.setNotes("Old mentoring session");
		oldInteraction.setOccurredAt(
				LocalDate.of(2024, 12, 1).atStartOfDay(ZoneId.of("UTC")).toInstant());
		interactionRepository.save(oldInteraction);
	}

	@Test
	void getMatrix_returns200WithCorrectDataForAllEmployees() throws Exception {
		// Fixed reference date: 2025-01-15
		// No Interaction Employee: recency=null, frequency=0, status=OVERDUE, followUp=true
		// Recent Employee: recency=5, frequency=1, status=ON_TRACK, followUp=false, lastInteractionDate=2025-01-10
		// Old Employee: recency=45, frequency=1, status=OVERDUE, followUp=true, lastInteractionDate=2024-12-01
		mockMvc.perform(get("/api/engagement/matrix")
						.with(user("test@test.com"))
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$.length()").value(3))
				// Default sort is by name ascending (case-insensitive):
				// "No Interaction Employee", "Old Employee", "Recent Employee"
				.andExpect(jsonPath("$[0].employeeName").value("No Interaction Employee"))
				.andExpect(jsonPath("$[0].recency").isEmpty())
				.andExpect(jsonPath("$[0].frequency").value(0))
				.andExpect(jsonPath("$[0].lastInteractionDate").isEmpty())
				.andExpect(jsonPath("$[0].engagementStatus").value("OVERDUE"))
				.andExpect(jsonPath("$[0].followUpRequired").value(true))
				.andExpect(jsonPath("$[1].employeeName").value("Old Employee"))
				.andExpect(jsonPath("$[1].recency").value(45))
				.andExpect(jsonPath("$[1].frequency").value(1))
				.andExpect(jsonPath("$[1].lastInteractionDate").value("2024-12-01"))
				.andExpect(jsonPath("$[1].engagementStatus").value("OVERDUE"))
				.andExpect(jsonPath("$[1].followUpRequired").value(true))
				.andExpect(jsonPath("$[2].employeeName").value("Recent Employee"))
				.andExpect(jsonPath("$[2].recency").value(5))
				.andExpect(jsonPath("$[2].frequency").value(1))
				.andExpect(jsonPath("$[2].lastInteractionDate").value("2025-01-10"))
				.andExpect(jsonPath("$[2].engagementStatus").value("ON_TRACK"))
				.andExpect(jsonPath("$[2].followUpRequired").value(false));
	}

	@Test
	void getMatrix_withStatusOverdue_returnsOnlyOverdueEmployees() throws Exception {
		mockMvc.perform(get("/api/engagement/matrix")
						.param("status", "OVERDUE")
						.with(user("test@test.com"))
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$.length()").value(2))
				// Sorted by name: "No Interaction Employee", "Old Employee"
				.andExpect(jsonPath("$[0].employeeName").value("No Interaction Employee"))
				.andExpect(jsonPath("$[0].engagementStatus").value("OVERDUE"))
				.andExpect(jsonPath("$[1].employeeName").value("Old Employee"))
				.andExpect(jsonPath("$[1].engagementStatus").value("OVERDUE"));
	}

	@Test
	void getMatrix_returnsValidJsonArrayStructure() throws Exception {
		mockMvc.perform(get("/api/engagement/matrix")
						.with(user("test@test.com"))
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$[0].employeeId").isNumber())
				.andExpect(jsonPath("$[0].employeeName").isString())
				.andExpect(jsonPath("$[0].employeeEmail").isString())
				.andExpect(jsonPath("$[0].engagementStatus").isString())
				.andExpect(jsonPath("$[0].followUpRequired").isBoolean());
	}
}
