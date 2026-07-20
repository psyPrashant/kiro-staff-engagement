package com.psybergate.staff_engagement.scheduling.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.psybergate.staff_engagement.employee.domain.Employee;
import com.psybergate.staff_engagement.employee.domain.EmployeeRepository;
import com.psybergate.staff_engagement.interaction.domain.InteractionType;
import com.psybergate.staff_engagement.scheduling.domain.ScheduledInteraction;
import com.psybergate.staff_engagement.scheduling.domain.ScheduledInteractionRepository;
import com.psybergate.staff_engagement.scheduling.dto.CreateScheduledInteractionRequest;
import com.psybergate.staff_engagement.scheduling.dto.ScheduledInteractionResponse;
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
 * Property-based tests for date validation in SchedulingService.
 * <p>
 * Property 2: Date Validation Rejects Past Dates
 * <p>
 * For any LocalDate that is strictly before the current reference date (today),
 * attempting to create a ScheduledInteraction with that date SHALL result in an
 * IllegalArgumentException. For any LocalDate that is equal to or after today,
 * the date validation SHALL pass without exception.
 * <p>
 * <b>Validates: Requirements 1.8, 2.6, 4.6, 10.1, 10.2</b>
 */
@Tag("Feature: interaction-scheduling, Property 2: Date Validation Rejects Past Dates")
class DateValidationPropertyTest {

	private static final LocalDate REFERENCE_DATE = LocalDate.of(2025, 1, 15);
	private static final Clock FIXED_CLOCK = Clock.fixed(
			REFERENCE_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant(),
			ZoneId.systemDefault());

	private SchedulingService service;
	private ScheduledInteractionRepository repository;
	private EmployeeRepository employeeRepository;
	private UserRepository userRepository;

	@BeforeProperty
	void setUp() {
		repository = Mockito.mock(ScheduledInteractionRepository.class);
		employeeRepository = Mockito.mock(EmployeeRepository.class);
		userRepository = Mockito.mock(UserRepository.class);

		Employee employee = new Employee();
		employee.setId(1L);
		employee.setName("Test Employee");
		employee.setEmail("test@example.com");

		User user = new User();
		user.setId(1L);

		when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(repository.save(any(ScheduledInteraction.class))).thenAnswer(invocation -> {
			ScheduledInteraction entity = invocation.getArgument(0);
			entity.setId(1L);
			return entity;
		});

		service = new SchedulingServiceImpl(repository, employeeRepository, userRepository, FIXED_CLOCK);
	}

	// **Validates: Requirements 1.8, 2.6, 4.6, 10.1, 10.2**

	@Property(tries = 100)
	void pastDatesShouldThrowIllegalArgumentException(
			@ForAll("pastDates") LocalDate pastDate) {

		CreateScheduledInteractionRequest request = new CreateScheduledInteractionRequest(
				1L, pastDate, InteractionType.CHECK_IN, null);

		assertThatThrownBy(() -> service.create(request, 1L))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Scheduled date must be today or in the future");
	}

	@Property(tries = 100)
	void todayAndFutureDatesShouldPassDateValidation(
			@ForAll("todayOrFutureDates") LocalDate validDate) {

		CreateScheduledInteractionRequest request = new CreateScheduledInteractionRequest(
				1L, validDate, InteractionType.CHECK_IN, null);

		// Should not throw IllegalArgumentException for date validation.
		// The call may succeed fully (return a response) since employee and user exist.
		ScheduledInteractionResponse response = service.create(request, 1L);
		assertThat(response).isNotNull();
		assertThat(response.scheduledDate()).isEqualTo(validDate);
	}

	@Provide
	Arbitrary<LocalDate> pastDates() {
		// Generate dates from 1 to 3650 days before the reference date
		return Arbitraries.integers().between(1, 3650)
				.map(REFERENCE_DATE::minusDays);
	}

	@Provide
	Arbitrary<LocalDate> todayOrFutureDates() {
		// Generate dates from 0 (today) to 365 days after the reference date
		return Arbitraries.integers().between(0, 365)
				.map(REFERENCE_DATE::plusDays);
	}
}
