package com.psybergate.staff_engagement.scheduling;

import com.psybergate.staff_engagement.employee.Employee;
import com.psybergate.staff_engagement.employee.EmployeeRepository;
import com.psybergate.staff_engagement.interaction.InteractionType;
import com.psybergate.staff_engagement.user.User;
import com.psybergate.staff_engagement.user.UserRepository;
import net.jqwik.api.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Property-based test for list ordering invariant.
 *
 * Feature: interaction-scheduling, Property 5: List Ordering Invariant
 * **Validates: Requirements 3.2**
 */
@Tag("Feature: interaction-scheduling, Property 5: List Ordering Invariant")
class ListOrderingPropertyTest {

	private static final LocalDate REFERENCE_DATE = LocalDate.of(2025, 6, 15);
	private static final Long USER_ID = 1L;

	@Property(tries = 200)
	void listAlwaysReturnsSortedByScheduledDateAscending(
			@ForAll("randomScheduledInteractionLists") List<ScheduledInteraction> unsortedEntities) {

		// Simulate what the repository does: sort by scheduledDate ASC
		List<ScheduledInteraction> sortedEntities = unsortedEntities.stream()
				.sorted(Comparator.comparing(ScheduledInteraction::getScheduledDate))
				.collect(Collectors.toList());

		// Mock the repository to return the sorted list (as the DB would)
		ScheduledInteractionRepository repository = mock(ScheduledInteractionRepository.class);
		when(repository.findByScheduledByIdOrderByScheduledDateAsc(USER_ID))
				.thenReturn(sortedEntities);

		EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
		UserRepository userRepository = mock(UserRepository.class);

		Clock fixedClock = Clock.fixed(
				REFERENCE_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant(),
				ZoneId.systemDefault());

		SchedulingService service = new SchedulingService(
				repository, employeeRepository, userRepository, fixedClock);

		// Call list with no filters
		List<ScheduledInteractionResponse> results = service.list(USER_ID, null, null, null);

		// Assert: for every consecutive pair, entries[i].scheduledDate <= entries[i+1].scheduledDate
		for (int i = 0; i < results.size() - 1; i++) {
			LocalDate current = results.get(i).scheduledDate();
			LocalDate next = results.get(i + 1).scheduledDate();
			assertThat(current).isBeforeOrEqualTo(next);
		}
	}

	@Provide
	Arbitrary<List<ScheduledInteraction>> randomScheduledInteractionLists() {
		return scheduledInteractionArbitrary()
				.list()
				.ofMinSize(2)
				.ofMaxSize(20);
	}

	private Arbitrary<ScheduledInteraction> scheduledInteractionArbitrary() {
		Arbitrary<LocalDate> dates = Arbitraries.integers()
				.between(-180, 365)
				.map(offset -> REFERENCE_DATE.plusDays(offset));

		Arbitrary<InteractionType> types = Arbitraries.of(InteractionType.values());

		return Combinators.combine(dates, types, Arbitraries.longs().between(1L, 1000L))
				.as((date, type, entityId) -> {
					ScheduledInteraction entity = new ScheduledInteraction();
					entity.setId(entityId);
					entity.setScheduledDate(date);
					entity.setInteractionType(type);
					entity.setCompletionStatus(CompletionStatus.PENDING);
					entity.setNotes(null);
					entity.setCreatedAt(Instant.now());

					// Set up employee with name for toResponse mapping
					Employee employee = new Employee();
					employee.setId(10L);
					employee.setName("Test Employee");
					entity.setEmployee(employee);

					// Set up user for scheduledBy
					User user = new User();
					user.setId(USER_ID);
					user.setName("Test User");
					entity.setScheduledBy(user);

					return entity;
				});
	}
}
