package com.psybergate.staff_engagement.scheduling.property;

import static org.assertj.core.api.Assertions.assertThat;

import com.psybergate.staff_engagement.employee.domain.Employee;
import com.psybergate.staff_engagement.employee.domain.EmployeeRepository;
import com.psybergate.staff_engagement.interaction.domain.InteractionType;
import com.psybergate.staff_engagement.scheduling.domain.CompletionStatus;
import com.psybergate.staff_engagement.scheduling.domain.ScheduledInteraction;
import com.psybergate.staff_engagement.scheduling.domain.ScheduledInteractionRepository;
import com.psybergate.staff_engagement.scheduling.dto.NextScheduledDto;
import com.psybergate.staff_engagement.scheduling.service.NextScheduledInteractionService;
import com.psybergate.staff_engagement.support.TestcontainersConfiguration;
import com.psybergate.staff_engagement.user.domain.User;
import com.psybergate.staff_engagement.user.domain.UserRepository;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/**
 * Property-based test: Batch method consistency with single method.
 * <p>
 * Property 3: For any list of employee IDs (size 1–200), the result of
 * getNextScheduledBatch(ids) for each employee ID SHALL be identical to
 * calling getNextScheduled(id) individually — i.e. the map entry for each
 * employee matches the DTO that the single-employee method would produce
 * given the same underlying data and reference date.
 * <p>
 * <b>Validates: Requirements 2.6, 2.7</b>
 * <p>
 * Uses @SpringBootTest with Testcontainers (PostgreSQL) since jqwik-spring is not available.
 * Runs 100 trials with randomized data to simulate property-based testing.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Tag("Feature: next-scheduled-interaction, Property 3: Batch consistency with single method")
class NextScheduledBatchConsistencyPropertyTest {

	@Autowired
	private ScheduledInteractionRepository scheduledInteractionRepository;

	@Autowired
	private EmployeeRepository employeeRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private NextScheduledInteractionService nextScheduledInteractionService;

	private User user;
	private List<Employee> employees;

	private static final InteractionType[] INTERACTION_TYPES = InteractionType.values();
	private static final CompletionStatus[] COMPLETION_STATUSES = CompletionStatus.values();
	private static final int EMPLOYEE_POOL_SIZE = 10;

	@BeforeEach
	void setUp() {
		scheduledInteractionRepository.deleteAll();

		// Ensure we have a user for scheduling interactions
		if (userRepository.count() == 0) {
			User usr = new User();
			usr.setName("Batch Property Test User");
			usr.setEmail("batch.prop.test.user@example.com");
			usr.setPasswordHash("hashed");
			user = userRepository.save(usr);
		} else {
			user = userRepository.findAll().get(0);
		}

		// Create a pool of employees for randomized testing
		long existingCount = employeeRepository.count();
		employees = new ArrayList<>(employeeRepository.findAll());

		for (long i = existingCount; i < EMPLOYEE_POOL_SIZE; i++) {
			Employee emp = new Employee();
			emp.setName("Batch Test Employee " + i);
			emp.setEmail("batch.test.employee." + i + "@example.com");
			employees.add(employeeRepository.save(emp));
		}
	}

	/**
	 * Property 3: Batch method consistency with single method.
	 * <p>
	 * Validates: Requirements 2.6, 2.7
	 */
	@Test
	@Transactional
	void batchMethodIsConsistentWithSingleMethod() {
		Random random = new Random(67890);
		LocalDate baseDate = LocalDate.of(2025, 6, 1);

		for (int trial = 0; trial < 100; trial++) {
			scheduledInteractionRepository.deleteAll();
			scheduledInteractionRepository.flush();

			// Seed random interactions for a random subset of employees
			int interactionsToCreate = 1 + random.nextInt(20);
			for (int i = 0; i < interactionsToCreate; i++) {
				ScheduledInteraction si = new ScheduledInteraction();
				si.setEmployee(employees.get(random.nextInt(employees.size())));
				si.setScheduledBy(user);

				// Random date: baseDate - 15 to baseDate + 45
				int dateOffset = random.nextInt(61) - 15;
				si.setScheduledDate(baseDate.plusDays(dateOffset));

				si.setInteractionType(INTERACTION_TYPES[random.nextInt(INTERACTION_TYPES.length)]);
				si.setCompletionStatus(COMPLETION_STATUSES[random.nextInt(COMPLETION_STATUSES.length)]);

				scheduledInteractionRepository.saveAndFlush(si);
			}

			// Pick a random subset of employee IDs (1 to EMPLOYEE_POOL_SIZE)
			int subsetSize = 1 + random.nextInt(employees.size());
			List<Long> selectedIds = employees.stream()
					.map(Employee::getId)
					.collect(Collectors.toList());
			Collections.shuffle(selectedIds, random);
			selectedIds = selectedIds.subList(0, subsetSize);

			// Call the batch method
			Map<Long, NextScheduledDto> batchResult =
					nextScheduledInteractionService.getNextScheduledBatch(selectedIds);

			// For each selected ID, call the single method and compare
			for (Long employeeId : selectedIds) {
				NextScheduledDto singleResult =
						nextScheduledInteractionService.getNextScheduled(employeeId);
				NextScheduledDto batchEntry = batchResult.get(employeeId);

				if (singleResult == null) {
					assertThat(batchEntry)
							.as("Trial %d, employee %d: batch should return null when single returns null",
									trial, employeeId)
							.isNull();
				} else {
					assertThat(batchEntry)
							.as("Trial %d, employee %d: batch should return non-null when single returns non-null",
									trial, employeeId)
							.isNotNull();

					assertThat(batchEntry.scheduledAt())
							.as("Trial %d, employee %d: scheduledAt should match between batch and single",
									trial, employeeId)
							.isEqualTo(singleResult.scheduledAt());

					assertThat(batchEntry.type())
							.as("Trial %d, employee %d: type should match between batch and single",
									trial, employeeId)
							.isEqualTo(singleResult.type());
				}
			}
		}
	}
}
