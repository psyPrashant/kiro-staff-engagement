package com.psybergate.staff_engagement.engagement;

import com.psybergate.staff_engagement.employee.Employee;
import com.psybergate.staff_engagement.employee.EmployeeRepository;
import com.psybergate.staff_engagement.engagement.dto.EngagementMatrixEntry;
import com.psybergate.staff_engagement.interaction.InteractionRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import org.mockito.Mockito;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.when;

/**
 * Property-based tests for EngagementService logic.
 * Uses jqwik to verify universal properties across randomized inputs.
 */
class EngagementServicePropertyTest {

	private static final LocalDate REFERENCE_DATE = LocalDate.of(2025, 1, 15);
	private static final Clock FIXED_CLOCK = Clock.fixed(
			REFERENCE_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant(),
			ZoneId.systemDefault());

	private EngagementService createService(List<Employee> employees, List<Object[]> aggregates) {
		EmployeeRepository employeeRepo = Mockito.mock(EmployeeRepository.class);
		InteractionRepository interactionRepo = Mockito.mock(InteractionRepository.class);
		EngagementThresholdProperties thresholds = new EngagementThresholdProperties();
		thresholds.setOverdueDays(30);
		thresholds.setAtRiskDays(14);

		when(employeeRepo.findAll()).thenReturn(employees);
		when(interactionRepo.findInteractionAggregatesByEmployee()).thenReturn(aggregates);

		return new EngagementService(employeeRepo, interactionRepo, thresholds, FIXED_CLOCK);
	}

	private Employee createEmployee(long id, String name) {
		Employee emp = new Employee();
		emp.setId(id);
		emp.setName(name);
		emp.setEmail(name.toLowerCase().replaceAll("\\s+", ".") + "@example.com");
		return emp;
	}

	// Feature: interaction-matrix-followups, Property 1: Matrix contains exactly one entry per employee
	// **Validates: Requirements 1.1**

	@Property(tries = 100)
	void matrixContainsExactlyOneEntryPerEmployee(
			@ForAll @IntRange(min = 0, max = 50) int employeeCount) {

		List<Employee> employees = IntStream.rangeClosed(1, employeeCount)
				.mapToObj(i -> createEmployee(i, "Employee" + i))
				.collect(Collectors.toList());

		// Generate some random aggregates (some employees may have interactions, some not)
		Random rng = new Random(employeeCount);
		List<Object[]> aggregates = new ArrayList<>();
		for (Employee emp : employees) {
			if (rng.nextBoolean()) {
				int daysAgo = rng.nextInt(60);
				Instant lastOccurred = REFERENCE_DATE.minusDays(daysAgo)
						.atStartOfDay(ZoneId.systemDefault()).toInstant();
				long count = rng.nextInt(20) + 1;
				aggregates.add(new Object[]{emp.getId(), lastOccurred, count});
			}
		}

		EngagementService service = createService(employees, aggregates);

		List<EngagementMatrixEntry> result = service.computeMatrix(REFERENCE_DATE, null, null);

		// Verify result size equals employee count
		assertThat(result).hasSize(employeeCount);

		// Verify each employeeId appears exactly once
		Set<Long> returnedIds = result.stream()
				.map(EngagementMatrixEntry::employeeId)
				.collect(Collectors.toSet());
		Set<Long> expectedIds = employees.stream()
				.map(Employee::getId)
				.collect(Collectors.toSet());
		assertThat(returnedIds).isEqualTo(expectedIds);
	}

	// Feature: interaction-matrix-followups, Property 7: Recency sort ordering
	// **Validates: Requirements 5.2**

	@Property(tries = 100)
	void recencySortOrderingIsCorrect(
			@ForAll @IntRange(min = 1, max = 20) int employeeCount) {

		List<Employee> employees = IntStream.rangeClosed(1, employeeCount)
				.mapToObj(i -> createEmployee(i, "Employee" + i))
				.collect(Collectors.toList());

		// Generate aggregates with varying recency including some employees with no interactions (null recency)
		Random rng = new Random(employeeCount * 7);
		List<Object[]> aggregates = new ArrayList<>();
		for (Employee emp : employees) {
			// ~30% of employees have no interactions (null recency)
			if (rng.nextInt(10) >= 3) {
				int daysAgo = rng.nextInt(60);
				Instant lastOccurred = REFERENCE_DATE.minusDays(daysAgo)
						.atStartOfDay(ZoneId.systemDefault()).toInstant();
				long count = rng.nextInt(10) + 1;
				aggregates.add(new Object[]{emp.getId(), lastOccurred, count});
			}
		}

		EngagementService service = createService(employees, aggregates);

		List<EngagementMatrixEntry> result = service.computeMatrix(REFERENCE_DATE, null, "recency");

		// Verify: nulls come first, then descending by recency value
		for (int i = 0; i < result.size() - 1; i++) {
			Integer curr = result.get(i).recency();
			Integer next = result.get(i + 1).recency();
			if (curr == null) {
				// null is fine at the start - no constraint on next
			} else if (next == null) {
				fail("null recency should appear before non-null recency, but found non-null (%d) at index %d before null at index %d"
						.formatted(curr, i, i + 1));
			} else {
				assertThat(curr)
						.as("Recency at index %d (%d) should be >= recency at index %d (%d) for descending order",
								i, curr, i + 1, next)
						.isGreaterThanOrEqualTo(next);
			}
		}
	}

	// Feature: interaction-matrix-followups, Property 8: Default name sort ordering
	// **Validates: Requirements 5.3**

	@Property(tries = 100)
	void defaultNameSortOrderingIsCorrect(
			@ForAll @IntRange(min = 1, max = 20) int employeeCount) {

		// Generate employees with varied name casing to test case-insensitive sort
		String[] namePrefixes = {"Alice", "bob", "CAROL", "dave", "Eve", "FRANK",
				"grace", "HENRY", "Iris", "Jack", "Karen", "LEO",
				"Mia", "NATE", "Olivia", "PETER", "Quinn", "ROSA",
				"sam", "TINA"};

		Random rng = new Random(employeeCount * 8);
		List<Employee> employees = IntStream.rangeClosed(1, employeeCount)
				.mapToObj(i -> {
					String name = namePrefixes[rng.nextInt(namePrefixes.length)] + i;
					return createEmployee(i, name);
				})
				.collect(Collectors.toList());

		// All employees have some interactions (irrelevant for name sort test)
		List<Object[]> aggregates = new ArrayList<>();
		for (Employee emp : employees) {
			int daysAgo = rng.nextInt(60);
			Instant lastOccurred = REFERENCE_DATE.minusDays(daysAgo)
					.atStartOfDay(ZoneId.systemDefault()).toInstant();
			long count = rng.nextInt(10) + 1;
			aggregates.add(new Object[]{emp.getId(), lastOccurred, count});
		}

		EngagementService service = createService(employees, aggregates);

		// sort=null triggers default name sort
		List<EngagementMatrixEntry> result = service.computeMatrix(REFERENCE_DATE, null, null);

		// Verify case-insensitive ascending name order
		for (int i = 0; i < result.size() - 1; i++) {
			String currName = result.get(i).employeeName();
			String nextName = result.get(i + 1).employeeName();
			assertThat(currName.compareToIgnoreCase(nextName))
					.as("Name at index %d ('%s') should be <= name at index %d ('%s') in case-insensitive order",
							i, currName, i + 1, nextName)
					.isLessThanOrEqualTo(0);
		}
	}
}
