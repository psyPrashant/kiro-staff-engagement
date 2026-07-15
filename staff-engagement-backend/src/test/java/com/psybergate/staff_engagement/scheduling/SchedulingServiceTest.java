package com.psybergate.staff_engagement.scheduling;

import com.psybergate.staff_engagement.employee.Employee;
import com.psybergate.staff_engagement.employee.EmployeeRepository;
import com.psybergate.staff_engagement.interaction.InteractionType;
import com.psybergate.staff_engagement.user.User;
import com.psybergate.staff_engagement.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Fast unit tests for {@link SchedulingService}.
 *
 * <p>These complement the container-backed {@code SchedulingIntegrationTest} by asserting the
 * exact field values written on the persisted entity and the filtering/ownership logic — the
 * behaviour that survived mutation testing when only integration tests covered this service.
 */
@ExtendWith(MockitoExtension.class)
class SchedulingServiceTest {

	private static final Long USER_ID = 9L;
	// Fixed "today" so date-relative logic is deterministic.
	private static final LocalDate TODAY = LocalDate.of(2026, 1, 15);
	private final Clock clock = Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);

	@Mock
	private ScheduledInteractionRepository repository;
	@Mock
	private EmployeeRepository employeeRepository;
	@Mock
	private UserRepository userRepository;

	private SchedulingService service() {
		return new SchedulingService(repository, employeeRepository, userRepository, clock);
	}

	private Employee employee(long id, String name) {
		Employee e = new Employee();
		e.setId(id);
		e.setName(name);
		return e;
	}

	private User user(long id) {
		User u = new User();
		u.setId(id);
		return u;
	}

	private ScheduledInteraction entity(Long id, Employee employee, LocalDate date, CompletionStatus status) {
		ScheduledInteraction si = new ScheduledInteraction();
		si.setId(id);
		si.setEmployee(employee);
		si.setScheduledBy(user(USER_ID));
		si.setScheduledDate(date);
		si.setInteractionType(InteractionType.CHECK_IN);
		si.setCompletionStatus(status);
		si.setCreatedAt(Instant.now());
		return si;
	}

	// --- create -----------------------------------------------------------------

	@Test
	void createPersistsEntityWithAllRequestFieldsAndPendingStatus() {
		Employee jane = employee(1L, "Jane Doe");
		when(employeeRepository.findById(1L)).thenReturn(Optional.of(jane));
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID)));
		when(repository.save(any(ScheduledInteraction.class))).thenAnswer(inv -> {
			ScheduledInteraction s = inv.getArgument(0);
			s.setId(100L);
			return s;
		});

		LocalDate future = TODAY.plusDays(10);
		var request = new CreateScheduledInteractionRequest(1L, future, InteractionType.MENTORING, "prep notes");

		ScheduledInteractionResponse response = service().create(request, USER_ID);

		ArgumentCaptor<ScheduledInteraction> captor = ArgumentCaptor.forClass(ScheduledInteraction.class);
		verify(repository).save(captor.capture());
		ScheduledInteraction saved = captor.getValue();
		assertThat(saved.getEmployee()).isSameAs(jane);
		assertThat(saved.getScheduledBy().getId()).isEqualTo(USER_ID);
		assertThat(saved.getScheduledDate()).isEqualTo(future);
		assertThat(saved.getInteractionType()).isEqualTo(InteractionType.MENTORING);
		assertThat(saved.getNotes()).isEqualTo("prep notes");
		assertThat(saved.getCompletionStatus()).isEqualTo(CompletionStatus.PENDING);

		assertThat(response.id()).isEqualTo(100L);
		assertThat(response.employeeId()).isEqualTo(1L);
		assertThat(response.employeeName()).isEqualTo("Jane Doe");
		assertThat(response.scheduledDate()).isEqualTo(future);
		assertThat(response.interactionType()).isEqualTo(InteractionType.MENTORING);
		assertThat(response.completionStatus()).isEqualTo(CompletionStatus.PENDING);
		assertThat(response.notes()).isEqualTo("prep notes");
		assertThat(response.overdue()).isFalse();
	}

	@Test
	void createRejectsPastScheduledDateAndDoesNotSave() {
		var request = new CreateScheduledInteractionRequest(1L, TODAY.minusDays(1), InteractionType.CHECK_IN, null);

		assertThatThrownBy(() -> service().create(request, USER_ID))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("today or in the future");

		verify(repository, never()).save(any());
	}

	@Test
	void createThrowsWhenEmployeeMissing() {
		when(employeeRepository.findById(1L)).thenReturn(Optional.empty());
		var request = new CreateScheduledInteractionRequest(1L, TODAY.plusDays(1), InteractionType.CHECK_IN, null);

		assertThatThrownBy(() -> service().create(request, USER_ID))
				.isInstanceOf(EmployeeNotFoundException.class);
	}

	@Test
	void createThrowsWhenSchedulingUserMissing() {
		when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee(1L, "Jane Doe")));
		when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
		var request = new CreateScheduledInteractionRequest(1L, TODAY.plusDays(1), InteractionType.CHECK_IN, null);

		assertThatThrownBy(() -> service().create(request, USER_ID))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("User not found");
	}

	// --- list -------------------------------------------------------------------

	@Test
	void listWithNoFiltersReturnsAllForUser() {
		Employee jane = employee(1L, "Jane Doe");
		when(repository.findByScheduledByIdOrderByScheduledDateAsc(USER_ID))
				.thenReturn(List.of(
						entity(1L, jane, TODAY.plusDays(3), CompletionStatus.PENDING),
						entity(2L, jane, TODAY.plusDays(5), CompletionStatus.COMPLETED)));

		List<ScheduledInteractionResponse> result = service().list(USER_ID, null, null, null);

		assertThat(result).hasSize(2);
		assertThat(result).extracting(ScheduledInteractionResponse::id).containsExactly(1L, 2L);
	}

	@Test
	void listWithStatusFilterDelegatesToStatusQuery() {
		Employee jane = employee(1L, "Jane Doe");
		when(repository.findByScheduledByIdAndCompletionStatusOrderByScheduledDateAsc(USER_ID, CompletionStatus.PENDING))
				.thenReturn(List.of(entity(1L, jane, TODAY.plusDays(3), CompletionStatus.PENDING)));

		List<ScheduledInteractionResponse> result = service().list(USER_ID, CompletionStatus.PENDING, null, null);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).completionStatus()).isEqualTo(CompletionStatus.PENDING);
	}

	@Test
	void listWithEmployeeFilterDelegatesToEmployeeQuery() {
		Employee jane = employee(1L, "Jane Doe");
		when(repository.findByScheduledByIdAndEmployeeIdOrderByScheduledDateAsc(USER_ID, 1L))
				.thenReturn(List.of(entity(1L, jane, TODAY.plusDays(3), CompletionStatus.PENDING)));

		List<ScheduledInteractionResponse> result = service().list(USER_ID, null, 1L, null);

		assertThat(result).extracting(ScheduledInteractionResponse::employeeId).containsExactly(1L);
	}

	@Test
	void listWithStatusAndEmployeeFilterDelegatesToCombinedQuery() {
		Employee jane = employee(1L, "Jane Doe");
		when(repository.findByScheduledByIdAndCompletionStatusAndEmployeeIdOrderByScheduledDateAsc(
				USER_ID, CompletionStatus.PENDING, 1L))
				.thenReturn(List.of(entity(1L, jane, TODAY.plusDays(3), CompletionStatus.PENDING)));

		List<ScheduledInteractionResponse> result =
				service().list(USER_ID, CompletionStatus.PENDING, 1L, null);

		assertThat(result).hasSize(1);
	}

	@Test
	void listWithOverdueTrueKeepsOnlyPastPendingEntries() {
		Employee jane = employee(1L, "Jane Doe");
		ScheduledInteraction overduePending = entity(1L, jane, TODAY.minusDays(2), CompletionStatus.PENDING);
		ScheduledInteraction futurePending = entity(2L, jane, TODAY.plusDays(2), CompletionStatus.PENDING);
		ScheduledInteraction pastCompleted = entity(3L, jane, TODAY.minusDays(2), CompletionStatus.COMPLETED);
		when(repository.findByScheduledByIdOrderByScheduledDateAsc(USER_ID))
				.thenReturn(List.of(overduePending, futurePending, pastCompleted));

		List<ScheduledInteractionResponse> result = service().list(USER_ID, null, null, true);

		assertThat(result).extracting(ScheduledInteractionResponse::id).containsExactly(1L);
		assertThat(result.get(0).overdue()).isTrue();
	}

	// --- update -----------------------------------------------------------------

	@Test
	void updateAppliesNotesDateAndStatusForOwner() {
		Employee jane = employee(1L, "Jane Doe");
		ScheduledInteraction existing = entity(7L, jane, TODAY.plusDays(1), CompletionStatus.PENDING);
		when(repository.findById(7L)).thenReturn(Optional.of(existing));
		when(repository.save(any(ScheduledInteraction.class))).thenAnswer(inv -> inv.getArgument(0));

		LocalDate newDate = TODAY.plusDays(20);
		var request = new UpdateScheduledInteractionRequest(CompletionStatus.COMPLETED, newDate, "done early");

		ScheduledInteractionResponse response = service().update(7L, request, USER_ID);

		ArgumentCaptor<ScheduledInteraction> captor = ArgumentCaptor.forClass(ScheduledInteraction.class);
		verify(repository).save(captor.capture());
		ScheduledInteraction saved = captor.getValue();
		assertThat(saved.getScheduledDate()).isEqualTo(newDate);
		assertThat(saved.getNotes()).isEqualTo("done early");
		assertThat(saved.getCompletionStatus()).isEqualTo(CompletionStatus.COMPLETED);
		assertThat(response.completionStatus()).isEqualTo(CompletionStatus.COMPLETED);
		assertThat(response.notes()).isEqualTo("done early");
	}

	@Test
	void updateByNonOwnerIsRejected() {
		Employee jane = employee(1L, "Jane Doe");
		ScheduledInteraction ownedByOther = entity(7L, jane, TODAY.plusDays(1), CompletionStatus.PENDING);
		ownedByOther.setScheduledBy(user(999L)); // different owner
		when(repository.findById(7L)).thenReturn(Optional.of(ownedByOther));

		var request = new UpdateScheduledInteractionRequest(CompletionStatus.COMPLETED, null, null);

		assertThatThrownBy(() -> service().update(7L, request, USER_ID))
				.isInstanceOf(ScheduledInteractionNotFoundException.class);
		verify(repository, never()).save(any());
	}

	// --- countOverdue -----------------------------------------------------------

	@Test
	void countOverdueDelegatesToRepositoryWithPendingBeforeToday() {
		when(repository.countByScheduledByIdAndCompletionStatusAndScheduledDateBefore(
				USER_ID, CompletionStatus.PENDING, TODAY)).thenReturn(4L);

		assertThat(service().countOverdue(USER_ID)).isEqualTo(4L);
	}
}
