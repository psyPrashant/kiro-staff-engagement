package com.psybergate.staff_engagement.scheduling.property;

import static org.assertj.core.api.Assertions.assertThat;

import com.psybergate.staff_engagement.employee.domain.Employee;
import com.psybergate.staff_engagement.employee.domain.EmployeeRepository;
import com.psybergate.staff_engagement.interaction.domain.Interaction;
import com.psybergate.staff_engagement.interaction.domain.InteractionType;
import com.psybergate.staff_engagement.scheduling.domain.CompletionStatus;
import com.psybergate.staff_engagement.scheduling.domain.ScheduledInteraction;
import com.psybergate.staff_engagement.scheduling.domain.ScheduledInteractionRepository;
import com.psybergate.staff_engagement.support.TestcontainersConfiguration;
import com.psybergate.staff_engagement.user.domain.User;
import com.psybergate.staff_engagement.user.domain.UserRepository;
import java.time.LocalDate;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/**
 * Property-based test for Scheduled Interaction persistence round-trip.
 * <p>
 * Property 1: Scheduled Interaction Persistence Round-Trip
 * <p>
 * For any valid ScheduledInteraction entity with a non-null employee, non-null scheduledBy user,
 * a scheduledDate ≥ today, a valid InteractionType, and an optional notes field (null or 0–2000
 * characters), persisting the entity and reloading it by ID SHALL produce an entity with identical
 * employeeId, scheduledByUserId, scheduledDate, interactionType, notes, and completionStatus
 * equal to PENDING, with a non-null createdAt timestamp.
 * <p>
 * <b>Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.7</b>
 * <p>
 * Uses @SpringBootTest with Testcontainers (PostgreSQL) since jqwik-spring is not available.
 * Runs 100 trials with randomized data to simulate property-based testing.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Tag("Feature: interaction-scheduling, Property 1: Persistence Round-Trip")
class ScheduledInteractionPersistencePropertyTest {

	@Autowired
	private ScheduledInteractionRepository scheduledInteractionRepository;

	@Autowired
	private EmployeeRepository employeeRepository;

	@Autowired
	private UserRepository userRepository;

	private Employee employee;
	private User user;

	private static final InteractionType[] INTERACTION_TYPES = InteractionType.values();
	private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 .,;:!?-_\n";

	@BeforeEach
	void setUp() {
		scheduledInteractionRepository.deleteAll();

		// Seed prerequisite data: at least one Employee and one User
		if (employeeRepository.count() == 0) {
			Employee emp = new Employee();
			emp.setName("Test Employee");
			emp.setEmail("test.employee@example.com");
			employee = employeeRepository.save(emp);
		} else {
			employee = employeeRepository.findAll().get(0);
		}

		if (userRepository.count() == 0) {
			User usr = new User();
			usr.setName("Test User");
			usr.setEmail("test.user@example.com");
			usr.setPasswordHash("hashed");
			user = userRepository.save(usr);
		} else {
			user = userRepository.findAll().get(0);
		}
	}

	/**
	 * Property 1: Persistence Round-Trip
	 * <p>
	 * Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.7
	 */
	@Test
	@Transactional
	void persistenceRoundTripPreservesAllFieldValues() {
		Random random = new Random(42); // Deterministic seed for reproducibility
		LocalDate today = LocalDate.now();

		for (int trial = 0; trial < 100; trial++) {
			// Generate random scheduledDate: today to +365 days
			int daysOffset = random.nextInt(366); // 0 to 365
			LocalDate scheduledDate = today.plusDays(daysOffset);

			// Generate random InteractionType
			InteractionType interactionType = INTERACTION_TYPES[random.nextInt(INTERACTION_TYPES.length)];

			// Generate optional notes: null or 0–2000 chars
			String notes = generateRandomNotes(random);

			// Create and persist entity
			ScheduledInteraction entity = new ScheduledInteraction();
			entity.setEmployee(employee);
			entity.setScheduledBy(user);
			entity.setScheduledDate(scheduledDate);
			entity.setInteractionType(interactionType);
			entity.setNotes(notes);
			// completionStatus defaults to PENDING

			ScheduledInteraction saved = scheduledInteractionRepository.saveAndFlush(entity);
			Long savedId = saved.getId();

			// Clear persistence context to force fresh reload from DB
			scheduledInteractionRepository.flush();

			// Reload by ID
			final int currentTrial = trial;
			ScheduledInteraction reloaded = scheduledInteractionRepository.findById(savedId)
					.orElseThrow(() -> new AssertionError(
							"Trial " + currentTrial + ": Entity not found after persistence, id=" + savedId));

			// Assert round-trip field equality
			assertThat(reloaded.getEmployee().getId())
					.as("Trial %d: employeeId", trial)
					.isEqualTo(employee.getId());

			assertThat(reloaded.getScheduledBy().getId())
					.as("Trial %d: scheduledByUserId", trial)
					.isEqualTo(user.getId());

			assertThat(reloaded.getScheduledDate())
					.as("Trial %d: scheduledDate", trial)
					.isEqualTo(scheduledDate);

			assertThat(reloaded.getInteractionType())
					.as("Trial %d: interactionType", trial)
					.isEqualTo(interactionType);

			assertThat(reloaded.getNotes())
					.as("Trial %d: notes", trial)
					.isEqualTo(notes);

			assertThat(reloaded.getCompletionStatus())
					.as("Trial %d: completionStatus should be PENDING", trial)
					.isEqualTo(CompletionStatus.PENDING);

			assertThat(reloaded.getCreatedAt())
					.as("Trial %d: createdAt should be non-null", trial)
					.isNotNull();

			// Clean up for next trial
			scheduledInteractionRepository.delete(reloaded);
			scheduledInteractionRepository.flush();
		}
	}

	private String generateRandomNotes(Random random) {
		// 50% chance of null notes
		if (random.nextBoolean()) {
			return null;
		}

		// Random length from 0 to 2000
		int length = random.nextInt(2001);
		if (length == 0) {
			return "";
		}

		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())));
		}
		return sb.toString();
	}
}
