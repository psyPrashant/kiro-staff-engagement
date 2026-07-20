package com.psybergate.staff_engagement.scheduling.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.psybergate.staff_engagement.employee.domain.Employee;
import com.psybergate.staff_engagement.interaction.domain.InteractionType;
import com.psybergate.staff_engagement.scheduling.domain.CompletionStatus;
import com.psybergate.staff_engagement.scheduling.domain.ScheduledInteraction;
import com.psybergate.staff_engagement.scheduling.domain.ScheduledInteractionRepository;
import com.psybergate.staff_engagement.scheduling.dto.ScheduledInteractionResponse;
import com.psybergate.staff_engagement.scheduling.service.SchedulingService;
import com.psybergate.staff_engagement.scheduling.service.SchedulingServiceImpl;
import com.psybergate.staff_engagement.user.domain.User;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;

/**
 * Property-based tests for filter correctness in SchedulingService.list().
 * <p>
 * Property 6: Filter Correctness
 * <p>
 * For any combination of status filter and employeeId filter applied to the list endpoint,
 * all returned entries SHALL satisfy: if status filter is set, entry.completionStatus == status;
 * if employeeId filter is set, entry.employeeId == employeeId; if overdue filter is true,
 * entry.overdue == true. Filters combine as logical AND.
 * <p>
 * <b>Validates: Requirements 3.3, 3.5, 5.3</b>
 */
@Tag("Feature: interaction-scheduling, Property 6: Filter Correctness")
class FilterCorrectnessPropertyTest {

	private static final LocalDate REFERENCE_DATE = LocalDate.of(2025, 6, 15);
	private static final Long USER_ID = 1L;
	private static final Long[] EMPLOYEE_IDS = {10L, 20L, 30L};

	private SchedulingService service;
	private ScheduledInteractionRepository repository;
	private Clock fixedClock;

	@BeforeProperty
	void setUp() {
		fixedClock = Clock.fixed(
				REFERENCE_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant(),
				ZoneId.systemDefault());

		repository = mock(ScheduledInteractionRepository.class);
		service = new SchedulingServiceImpl(repository, null, null, fixedClock);
	}

	/**
	 * **Validates: Requirements 3.3, 3.5, 5.3**
	 */
	@Property(tries = 200)
	void allReturnedEntriesSatisfyAllActiveFilterPredicates(
			@ForAll("statusFilters") CompletionStatus statusFilter,
			@ForAll("employeeIdFilters") Long employeeIdFilter,
			@ForAll("overdueFilters") Boolean overdueFilter) {

		// Build a diverse data set of ScheduledInteraction entities
		List<ScheduledInteraction> allEntities = buildDiverseDataSet();

		// Simulate what the repository would return given the status + employeeId filters
		List<ScheduledInteraction> repoResult = allEntities.stream()
				.filter(e -> e.getScheduledBy().getId().equals(USER_ID))
				.filter(e -> statusFilter == null || e.getCompletionStatus() == statusFilter)
				.filter(e -> employeeIdFilter == null || e.getEmployee().getId().equals(employeeIdFilter))
				.collect(Collectors.toList());

		// Set up mock repository to return the filtered subset
		setupMockRepository(statusFilter, employeeIdFilter, repoResult);

		// Call service.list with the generated filter combination
		List<ScheduledInteractionResponse> results = service.list(USER_ID, statusFilter, employeeIdFilter, overdueFilter);

		// Verify: ALL returned entries satisfy ALL active filter predicates
		for (ScheduledInteractionResponse entry : results) {
			// If status filter is set, entry must match that status
			if (statusFilter != null) {
				assertThat(entry.completionStatus())
						.as("Status filter active: entry should have status %s", statusFilter)
						.isEqualTo(statusFilter);
			}

			// If employeeId filter is set, entry must match that employeeId
			if (employeeIdFilter != null) {
				assertThat(entry.employeeId())
						.as("EmployeeId filter active: entry should have employeeId %d", employeeIdFilter)
						.isEqualTo(employeeIdFilter);
			}

			// If overdue filter is true, entry must be overdue
			if (Boolean.TRUE.equals(overdueFilter)) {
				assertThat(entry.overdue())
						.as("Overdue filter active: entry should be overdue")
						.isTrue();
				// Overdue means: scheduledDate < today AND status == PENDING
				assertThat(entry.completionStatus()).isEqualTo(CompletionStatus.PENDING);
				assertThat(entry.scheduledDate()).isBefore(REFERENCE_DATE);
			}
		}
	}

	@Provide
	Arbitrary<CompletionStatus> statusFilters() {
		// null means no filter applied, otherwise one of the enum values
		return Arbitraries.of(null, CompletionStatus.PENDING, CompletionStatus.COMPLETED, CompletionStatus.CANCELLED);
	}

	@Provide
	Arbitrary<Long> employeeIdFilters() {
		// null means no filter, otherwise pick from possible employee IDs
		// Include additional IDs to expand the combinatorial space for exhaustive generation
		return Arbitraries.of(null, 10L, 20L, 30L, 40L, 50L, 99L, 100L, 200L);
	}

	@Provide
	Arbitrary<Boolean> overdueFilters() {
		// null/false means no overdue filter, true means only overdue entries
		return Arbitraries.of(null, Boolean.FALSE, Boolean.TRUE);
	}

	private List<ScheduledInteraction> buildDiverseDataSet() {
		List<ScheduledInteraction> entities = new ArrayList<>();
		long idCounter = 1L;

		// Create entities with all combinations of status, employee, and dates (past/future)
		for (Long employeeId : EMPLOYEE_IDS) {
			for (CompletionStatus status : CompletionStatus.values()) {
				// Past date (overdue if PENDING)
				entities.add(createEntity(idCounter++, employeeId, status,
						REFERENCE_DATE.minusDays(5)));
				// Future date (never overdue)
				entities.add(createEntity(idCounter++, employeeId, status,
						REFERENCE_DATE.plusDays(10)));
				// Today (not overdue - same day is not before)
				entities.add(createEntity(idCounter++, employeeId, status,
						REFERENCE_DATE));
			}
		}

		return entities;
	}

	private ScheduledInteraction createEntity(Long id, Long employeeId,
			CompletionStatus status, LocalDate scheduledDate) {
		Employee employee = new Employee();
		employee.setId(employeeId);
		employee.setName("Employee-" + employeeId);

		User user = new User();
		user.setId(USER_ID);

		ScheduledInteraction entity = new ScheduledInteraction();
		entity.setId(id);
		entity.setEmployee(employee);
		entity.setScheduledBy(user);
		entity.setScheduledDate(scheduledDate);
		entity.setInteractionType(InteractionType.CHECK_IN);
		entity.setCompletionStatus(status);
		entity.setNotes(null);

		return entity;
	}

	private void setupMockRepository(CompletionStatus status, Long employeeId,
			List<ScheduledInteraction> repoResult) {
		if (status != null && employeeId != null) {
			when(repository.findByScheduledByIdAndCompletionStatusAndEmployeeIdOrderByScheduledDateAsc(
					USER_ID, status, employeeId))
					.thenReturn(repoResult);
		} else if (status != null) {
			when(repository.findByScheduledByIdAndCompletionStatusOrderByScheduledDateAsc(
					USER_ID, status))
					.thenReturn(repoResult);
		} else if (employeeId != null) {
			when(repository.findByScheduledByIdAndEmployeeIdOrderByScheduledDateAsc(
					USER_ID, employeeId))
					.thenReturn(repoResult);
		} else {
			when(repository.findByScheduledByIdOrderByScheduledDateAsc(USER_ID))
					.thenReturn(repoResult);
		}
	}
}
