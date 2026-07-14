package com.psybergate.staff_engagement.scheduling;

import com.psybergate.staff_engagement.TestcontainersConfiguration;
import com.psybergate.staff_engagement.employee.Employee;
import com.psybergate.staff_engagement.employee.EmployeeRepository;
import com.psybergate.staff_engagement.interaction.InteractionType;
import com.psybergate.staff_engagement.user.User;
import com.psybergate.staff_engagement.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test: Repository returns the earliest qualifying interaction.
 * <p>
 * Property 1: For any employee and set of scheduled interactions with varying scheduledDate values,
 * completionStatus values, and entity IDs, the repository query findNextPendingByEmployeeId(employeeId, referenceDate)
 * SHALL return the single interaction whose scheduledDate is the minimum among all PENDING interactions
 * with scheduledDate >= referenceDate, using the lowest id as tiebreaker when dates are equal,
 * or return empty if no qualifying interaction exists.
 * <p>
 * <b>Validates: Requirements 1.1, 1.2, 1.3, 1.4</b>
 * <p>
 * Uses @SpringBootTest with Testcontainers (PostgreSQL) since jqwik-spring is not available.
 * Runs 100 trials with randomized data to simulate property-based testing.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Tag("Feature: next-scheduled-interaction, Property 1: Repository returns earliest qualifying interaction")
class NextScheduledQueryPropertyTest {

	@Autowired
	private ScheduledInteractionRepository scheduledInteractionRepository;

	@Autowired
	private EmployeeRepository employeeRepository;

	@Autowired
	private UserRepository userRepository;

	private Employee employee;
	private User user;

	private static final InteractionType[] INTERACTION_TYPES = InteractionType.values();
	private static final CompletionStatus[] COMPLETION_STATUSES = CompletionStatus.values();

	@BeforeEach
	void setUp() {
		scheduledInteractionRepository.deleteAll();

		if (employeeRepository.count() == 0) {
			Employee emp = new Employee();
			emp.setName("Property Test Employee");
			emp.setEmail("prop.test.employee@example.com");
			employee = employeeRepository.save(emp);
		} else {
			employee = employeeRepository.findAll().get(0);
		}

		if (userRepository.count() == 0) {
			User usr = new User();
			usr.setName("Property Test User");
			usr.setEmail("prop.test.user@example.com");
			usr.setPasswordHash("hashed");
			user = userRepository.save(usr);
		} else {
			user = userRepository.findAll().get(0);
		}
	}

	/**
	 * Property 1: Repository returns the earliest qualifying interaction.
	 * <p>
	 * Validates: Requirements 1.1, 1.2, 1.3, 1.4
	 */
	@Test
	@Transactional
	void repositoryReturnsEarliestQualifyingInteraction() {
		Random random = new Random(12345);
		LocalDate baseDate = LocalDate.of(2025, 6, 1);

		for (int trial = 0; trial < 100; trial++) {
			scheduledInteractionRepository.deleteAll();
			scheduledInteractionRepository.flush();

			// Generate a random reference date near the base
			int refOffset = random.nextInt(30); // 0 to 29 days after base
			LocalDate referenceDate = baseDate.plusDays(refOffset);

			// Generate a random number of interactions (1 to 10)
			int interactionCount = 1 + random.nextInt(10);
			List<ScheduledInteraction> persisted = new ArrayList<>();

			for (int i = 0; i < interactionCount; i++) {
				ScheduledInteraction si = new ScheduledInteraction();
				si.setEmployee(employee);
				si.setScheduledBy(user);

				// Random date: referenceDate - 30 to referenceDate + 60
				int dateOffset = random.nextInt(91) - 30;
				si.setScheduledDate(referenceDate.plusDays(dateOffset));

				si.setInteractionType(INTERACTION_TYPES[random.nextInt(INTERACTION_TYPES.length)]);
				si.setCompletionStatus(COMPLETION_STATUSES[random.nextInt(COMPLETION_STATUSES.length)]);

				persisted.add(scheduledInteractionRepository.saveAndFlush(si));
			}

			// Compute expected result: earliest PENDING with scheduledDate >= referenceDate, lowest ID tiebreaker
			Optional<ScheduledInteraction> expected = persisted.stream()
					.filter(si -> si.getCompletionStatus() == CompletionStatus.PENDING)
					.filter(si -> !si.getScheduledDate().isBefore(referenceDate))
					.min(Comparator.comparing(ScheduledInteraction::getScheduledDate)
							.thenComparing(ScheduledInteraction::getId));

			// Query repository
			Optional<ScheduledInteraction> actual = scheduledInteractionRepository
					.findNextPendingByEmployeeId(employee.getId(), referenceDate);

			// Assert
			if (expected.isEmpty()) {
				assertThat(actual)
						.as("Trial %d: no qualifying interaction should exist", trial)
						.isEmpty();
			} else {
				assertThat(actual)
						.as("Trial %d: should return a result", trial)
						.isPresent();

				ScheduledInteraction expectedEntity = expected.get();
				ScheduledInteraction actualEntity = actual.get();

				assertThat(actualEntity.getId())
						.as("Trial %d: should return the interaction with lowest ID among earliest date", trial)
						.isEqualTo(expectedEntity.getId());

				assertThat(actualEntity.getScheduledDate())
						.as("Trial %d: scheduledDate should match", trial)
						.isEqualTo(expectedEntity.getScheduledDate());

				assertThat(actualEntity.getCompletionStatus())
						.as("Trial %d: completionStatus should be PENDING", trial)
						.isEqualTo(CompletionStatus.PENDING);

				// Verify it truly is the earliest: no other qualifying interaction has an earlier date
				LocalDate actualDate = actualEntity.getScheduledDate();
				boolean noneEarlier = persisted.stream()
						.filter(si -> si.getCompletionStatus() == CompletionStatus.PENDING)
						.filter(si -> !si.getScheduledDate().isBefore(referenceDate))
						.filter(si -> !si.getId().equals(actualEntity.getId()))
						.noneMatch(si -> si.getScheduledDate().isBefore(actualDate));

				assertThat(noneEarlier)
						.as("Trial %d: no other qualifying interaction should have an earlier date", trial)
						.isTrue();

				// Verify tiebreaker: no other qualifying interaction has same date but lower ID
				boolean noLowerIdSameDate = persisted.stream()
						.filter(si -> si.getCompletionStatus() == CompletionStatus.PENDING)
						.filter(si -> !si.getScheduledDate().isBefore(referenceDate))
						.filter(si -> si.getScheduledDate().equals(actualDate))
						.noneMatch(si -> si.getId() < actualEntity.getId());

				assertThat(noLowerIdSameDate)
						.as("Trial %d: no other qualifying interaction should have same date with lower ID", trial)
						.isTrue();
			}
		}
	}
}
