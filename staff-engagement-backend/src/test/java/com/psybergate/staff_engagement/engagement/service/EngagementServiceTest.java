package com.psybergate.staff_engagement.engagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.psybergate.staff_engagement.employee.domain.Employee;
import com.psybergate.staff_engagement.employee.domain.EmployeeRepository;
import com.psybergate.staff_engagement.engagement.config.EngagementThresholdProperties;
import com.psybergate.staff_engagement.engagement.domain.EngagementStatus;
import com.psybergate.staff_engagement.engagement.dto.EngagementMatrixEntry;
import com.psybergate.staff_engagement.interaction.domain.InteractionRepository;
import java.time.*;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link EngagementService}.
 * Uses Mockito to mock dependencies and a fixed Clock for deterministic testing.
 * Validates Requirements: 1.1, 1.2, 1.3, 1.4, 5.1, 5.2, 5.3
 */
@ExtendWith(MockitoExtension.class)
class EngagementServiceTest {

	@Mock
	private EmployeeRepository employeeRepository;

	@Mock
	private InteractionRepository interactionRepository;

	@Mock
	private EngagementThresholdProperties thresholds;

	private Clock clock;
	private EngagementService service;

	/** Fixed reference: 2025-01-15 UTC */
	private static final Instant FIXED_INSTANT = Instant.parse("2025-01-15T00:00:00Z");
	private static final ZoneId ZONE = ZoneId.of("UTC");

	@BeforeEach
	void setUp() {
		clock = Clock.fixed(FIXED_INSTANT, ZONE);
		service = new EngagementServiceImpl(employeeRepository, interactionRepository, thresholds, clock);
	}

	private Employee employee(Long id, String name, String email) {
		Employee emp = new Employee();
		emp.setId(id);
		emp.setName(name);
		emp.setEmail(email);
		return emp;
	}

	@Nested
	@DisplayName("computeMatrix — basic computation")
	class BasicComputation {

		@Test
		@DisplayName("empty employee list → empty result")
		void emptyEmployeeList_returnsEmptyList() {
			when(employeeRepository.findAll()).thenReturn(Collections.emptyList());
			when(interactionRepository.findInteractionAggregatesByEmployee()).thenReturn(Collections.emptyList());

			List<EngagementMatrixEntry> result = service.computeMatrix(null, null, null);

			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("employee with no interactions → recency null, frequency 0, lastInteractionDate null, OVERDUE, followUpRequired true")
		void employeeNoInteractions_returnsOverdueWithNulls() {
			Employee alice = employee(1L, "Alice", "alice@example.com");
			when(employeeRepository.findAll()).thenReturn(List.of(alice));
			when(interactionRepository.findInteractionAggregatesByEmployee()).thenReturn(Collections.emptyList());
			when(thresholds.getOverdueDays()).thenReturn(30);
			when(thresholds.getAtRiskDays()).thenReturn(14);

			List<EngagementMatrixEntry> result = service.computeMatrix(null, null, null);

			assertThat(result).hasSize(1);
			EngagementMatrixEntry entry = result.get(0);
			assertThat(entry.employeeId()).isEqualTo(1L);
			assertThat(entry.employeeName()).isEqualTo("Alice");
			assertThat(entry.employeeEmail()).isEqualTo("alice@example.com");
			assertThat(entry.recency()).isNull();
			assertThat(entry.frequency()).isZero();
			assertThat(entry.lastInteractionDate()).isNull();
			assertThat(entry.engagementStatus()).isEqualTo(EngagementStatus.OVERDUE);
			assertThat(entry.followUpRequired()).isTrue();
		}

		@Test
		@DisplayName("employee with interactions → correct recency, frequency, lastInteractionDate computation")
		void employeeWithInteractions_computesCorrectly() {
			Employee bob = employee(2L, "Bob", "bob@example.com");
			when(employeeRepository.findAll()).thenReturn(List.of(bob));

			// Last interaction was 2025-01-10 (5 days before reference date 2025-01-15)
			Instant lastOccurredAt = Instant.parse("2025-01-10T12:00:00Z");
			Object[] aggregate = new Object[]{2L, lastOccurredAt, 7L};
			List<Object[]> aggregates = new java.util.ArrayList<>();
			aggregates.add(aggregate);
			when(interactionRepository.findInteractionAggregatesByEmployee()).thenReturn(aggregates);
			when(thresholds.getOverdueDays()).thenReturn(30);
			when(thresholds.getAtRiskDays()).thenReturn(14);

			List<EngagementMatrixEntry> result = service.computeMatrix(null, null, null);

			assertThat(result).hasSize(1);
			EngagementMatrixEntry entry = result.get(0);
			assertThat(entry.employeeId()).isEqualTo(2L);
			assertThat(entry.recency()).isEqualTo(5);
			assertThat(entry.frequency()).isEqualTo(7);
			assertThat(entry.lastInteractionDate()).isEqualTo(LocalDate.of(2025, 1, 10));
			assertThat(entry.engagementStatus()).isEqualTo(EngagementStatus.ON_TRACK);
			assertThat(entry.followUpRequired()).isFalse();
		}
	}

	@Nested
	@DisplayName("computeMatrix — status filter")
	class StatusFilter {

		@Test
		@DisplayName("status filter correctly includes matching entries and excludes non-matching")
		void statusFilter_includesOnlyMatching() {
			Employee alice = employee(1L, "Alice", "alice@example.com");
			Employee bob = employee(2L, "Bob", "bob@example.com");
			Employee carol = employee(3L, "Carol", "carol@example.com");

			when(employeeRepository.findAll()).thenReturn(List.of(alice, bob, carol));

			// Alice: no interactions → OVERDUE
			// Bob: interaction 5 days ago → ON_TRACK
			// Carol: interaction 20 days ago → AT_RISK
			Instant bobLast = Instant.parse("2025-01-10T12:00:00Z");
			Instant carolLast = Instant.parse("2024-12-26T12:00:00Z");

			Object[] bobAgg = new Object[]{2L, bobLast, 3L};
			Object[] carolAgg = new Object[]{3L, carolLast, 1L};
			List<Object[]> aggregates = List.of(bobAgg, carolAgg);
			when(interactionRepository.findInteractionAggregatesByEmployee()).thenReturn(aggregates);
			when(thresholds.getOverdueDays()).thenReturn(30);
			when(thresholds.getAtRiskDays()).thenReturn(14);

			// Filter by OVERDUE — should only include Alice
			List<EngagementMatrixEntry> result = service.computeMatrix(null, EngagementStatus.OVERDUE, null);

			assertThat(result).hasSize(1);
			assertThat(result.get(0).employeeName()).isEqualTo("Alice");
			assertThat(result.get(0).engagementStatus()).isEqualTo(EngagementStatus.OVERDUE);
		}
	}

	@Nested
	@DisplayName("computeMatrix — sorting")
	class Sorting {

		@Test
		@DisplayName("sort by recency — nulls first, then descending")
		void sortByRecency_nullsFirstDescending() {
			Employee alice = employee(1L, "Alice", "alice@example.com");
			Employee bob = employee(2L, "Bob", "bob@example.com");
			Employee carol = employee(3L, "Carol", "carol@example.com");

			when(employeeRepository.findAll()).thenReturn(List.of(alice, bob, carol));

			// Alice: no interactions → recency null
			// Bob: interaction 5 days ago → recency 5
			// Carol: interaction 20 days ago → recency 20
			Instant bobLast = Instant.parse("2025-01-10T12:00:00Z");
			Instant carolLast = Instant.parse("2024-12-26T12:00:00Z");

			Object[] bobAgg = new Object[]{2L, bobLast, 3L};
			Object[] carolAgg = new Object[]{3L, carolLast, 1L};
			List<Object[]> aggregates = List.of(bobAgg, carolAgg);
			when(interactionRepository.findInteractionAggregatesByEmployee()).thenReturn(aggregates);
			when(thresholds.getOverdueDays()).thenReturn(30);
			when(thresholds.getAtRiskDays()).thenReturn(14);

			List<EngagementMatrixEntry> result = service.computeMatrix(null, null, "recency");

			assertThat(result).hasSize(3);
			// Nulls first, then descending: Alice (null), Carol (20), Bob (5)
			assertThat(result.get(0).employeeName()).isEqualTo("Alice");
			assertThat(result.get(0).recency()).isNull();
			assertThat(result.get(1).employeeName()).isEqualTo("Carol");
			assertThat(result.get(1).recency()).isEqualTo(20);
			assertThat(result.get(2).employeeName()).isEqualTo("Bob");
			assertThat(result.get(2).recency()).isEqualTo(5);
		}

		@Test
		@DisplayName("default sort — by name case-insensitive ascending")
		void defaultSort_byNameCaseInsensitiveAscending() {
			Employee charlie = employee(1L, "charlie", "charlie@example.com");
			Employee alice = employee(2L, "Alice", "alice@example.com");
			Employee bob = employee(3L, "Bob", "bob@example.com");

			when(employeeRepository.findAll()).thenReturn(List.of(charlie, alice, bob));
			when(interactionRepository.findInteractionAggregatesByEmployee()).thenReturn(Collections.emptyList());
			when(thresholds.getOverdueDays()).thenReturn(30);
			when(thresholds.getAtRiskDays()).thenReturn(14);

			List<EngagementMatrixEntry> result = service.computeMatrix(null, null, null);

			assertThat(result).hasSize(3);
			// Case-insensitive: Alice, Bob, charlie
			assertThat(result.get(0).employeeName()).isEqualTo("Alice");
			assertThat(result.get(1).employeeName()).isEqualTo("Bob");
			assertThat(result.get(2).employeeName()).isEqualTo("charlie");
		}
	}
}
