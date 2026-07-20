package com.psybergate.staff_engagement.scheduling.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.psybergate.staff_engagement.employee.domain.Employee;
import com.psybergate.staff_engagement.employee.domain.EmployeeRepository;
import com.psybergate.staff_engagement.scheduling.domain.CompletionStatus;
import com.psybergate.staff_engagement.scheduling.domain.ScheduledInteraction;
import com.psybergate.staff_engagement.scheduling.domain.ScheduledInteractionRepository;
import com.psybergate.staff_engagement.scheduling.dto.ScheduledInteractionResponse;
import com.psybergate.staff_engagement.scheduling.dto.UpdateScheduledInteractionRequest;
import com.psybergate.staff_engagement.scheduling.service.SchedulingService;
import com.psybergate.staff_engagement.scheduling.service.SchedulingServiceImpl;
import com.psybergate.staff_engagement.user.domain.User;
import com.psybergate.staff_engagement.user.domain.UserRepository;
import java.time.*;
import java.util.Optional;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.mockito.Mockito;

/**
 * Property-based tests for completion status transition correctness in SchedulingService.
 * <p>
 * Property 3: Completion Status Transition Correctness
 * <p>
 * For any (currentStatus, targetStatus) pair from the set
 * {PENDING, COMPLETED, CANCELLED} × {PENDING, COMPLETED, CANCELLED},
 * a status transition SHALL succeed if and only if currentStatus is PENDING
 * AND targetStatus is either COMPLETED or CANCELLED. All other transitions
 * SHALL throw an IllegalStateException.
 * <p>
 * <b>Validates: Requirements 4.2, 4.3, 10.3, 10.4</b>
 */
@Tag("Feature: interaction-scheduling, Property 3: Completion Status Transition Correctness")
class StatusTransitionPropertyTest {

	private static final LocalDate REFERENCE_DATE = LocalDate.of(2025, 1, 15);
	private static final Clock FIXED_CLOCK = Clock.fixed(
			REFERENCE_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant(),
			ZoneId.systemDefault());
	private static final Long USER_ID = 1L;
	private static final Long ENTITY_ID = 100L;

	private SchedulingService service;
	private ScheduledInteractionRepository repository;
	private EmployeeRepository employeeRepository;
	private UserRepository userRepository;

	@BeforeProperty
	void setUp() {
		repository = Mockito.mock(ScheduledInteractionRepository.class);
		employeeRepository = Mockito.mock(EmployeeRepository.class);
		userRepository = Mockito.mock(UserRepository.class);

		when(repository.save(any(ScheduledInteraction.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service = new SchedulingServiceImpl(repository, employeeRepository, userRepository, FIXED_CLOCK);
	}

	// **Validates: Requirements 4.2, 4.3, 10.3, 10.4**

	@Property(tries = 100)
	void validTransitionsShouldSucceed(
			@ForAll("validTransitions") Tuple.Tuple2<CompletionStatus, CompletionStatus> transition) {

		CompletionStatus currentStatus = transition.get1();
		CompletionStatus targetStatus = transition.get2();

		ScheduledInteraction entity = createEntityWithStatus(currentStatus);
		when(repository.findById(ENTITY_ID)).thenReturn(Optional.of(entity));

		UpdateScheduledInteractionRequest request = new UpdateScheduledInteractionRequest(
				targetStatus, null, null);

		ScheduledInteractionResponse response = service.update(ENTITY_ID, request, USER_ID);
		assertThat(response).isNotNull();
		assertThat(response.completionStatus()).isEqualTo(targetStatus);
	}

	@Property(tries = 100)
	void invalidTransitionsShouldThrowIllegalStateException(
			@ForAll("invalidTransitions") Tuple.Tuple2<CompletionStatus, CompletionStatus> transition) {

		CompletionStatus currentStatus = transition.get1();
		CompletionStatus targetStatus = transition.get2();

		ScheduledInteraction entity = createEntityWithStatus(currentStatus);
		when(repository.findById(ENTITY_ID)).thenReturn(Optional.of(entity));

		UpdateScheduledInteractionRequest request = new UpdateScheduledInteractionRequest(
				targetStatus, null, null);

		assertThatThrownBy(() -> service.update(ENTITY_ID, request, USER_ID))
				.isInstanceOf(IllegalStateException.class);
	}

	@Provide
	Arbitrary<Tuple.Tuple2<CompletionStatus, CompletionStatus>> validTransitions() {
		// Valid transitions: PENDING → COMPLETED, PENDING → CANCELLED
		return Arbitraries.of(
				Tuple.of(CompletionStatus.PENDING, CompletionStatus.COMPLETED),
				Tuple.of(CompletionStatus.PENDING, CompletionStatus.CANCELLED)
		);
	}

	@Provide
	Arbitrary<Tuple.Tuple2<CompletionStatus, CompletionStatus>> invalidTransitions() {
		// Invalid transitions: all pairs where currentStatus != PENDING OR targetStatus == PENDING
		return Arbitraries.of(
				// PENDING → PENDING (target not in {COMPLETED, CANCELLED})
				Tuple.of(CompletionStatus.PENDING, CompletionStatus.PENDING),
				// COMPLETED → any
				Tuple.of(CompletionStatus.COMPLETED, CompletionStatus.PENDING),
				Tuple.of(CompletionStatus.COMPLETED, CompletionStatus.COMPLETED),
				Tuple.of(CompletionStatus.COMPLETED, CompletionStatus.CANCELLED),
				// CANCELLED → any
				Tuple.of(CompletionStatus.CANCELLED, CompletionStatus.PENDING),
				Tuple.of(CompletionStatus.CANCELLED, CompletionStatus.COMPLETED),
				Tuple.of(CompletionStatus.CANCELLED, CompletionStatus.CANCELLED)
		);
	}

	private ScheduledInteraction createEntityWithStatus(CompletionStatus status) {
		Employee employee = new Employee();
		employee.setId(1L);
		employee.setName("Test Employee");
		employee.setEmail("test@example.com");

		User user = new User();
		user.setId(USER_ID);

		ScheduledInteraction entity = new ScheduledInteraction();
		entity.setId(ENTITY_ID);
		entity.setEmployee(employee);
		entity.setScheduledBy(user);
		entity.setScheduledDate(REFERENCE_DATE.plusDays(5));
		entity.setInteractionType(com.psybergate.staff_engagement.interaction.domain.InteractionType.CHECK_IN);
		entity.setNotes(null);
		entity.setCompletionStatus(status);
		entity.setCreatedAt(Instant.now());
		return entity;
	}
}
