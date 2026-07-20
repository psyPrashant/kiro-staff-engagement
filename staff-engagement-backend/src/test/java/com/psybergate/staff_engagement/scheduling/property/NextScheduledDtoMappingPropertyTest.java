package com.psybergate.staff_engagement.scheduling.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.psybergate.staff_engagement.employee.domain.Employee;
import com.psybergate.staff_engagement.interaction.domain.InteractionType;
import com.psybergate.staff_engagement.scheduling.domain.CompletionStatus;
import com.psybergate.staff_engagement.scheduling.domain.ScheduledInteraction;
import com.psybergate.staff_engagement.scheduling.domain.ScheduledInteractionRepository;
import com.psybergate.staff_engagement.scheduling.dto.NextScheduledDto;
import com.psybergate.staff_engagement.scheduling.service.NextScheduledInteractionService;
import com.psybergate.staff_engagement.scheduling.service.NextScheduledInteractionServiceImpl;
import java.time.*;
import java.util.Optional;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.mockito.Mockito;

/**
 * Property-based tests for DTO mapping in NextScheduledInteractionService.
 * <p>
 * Property 2: Service DTO mapping round-trip
 * <p>
 * For any ScheduledInteraction entity with a valid scheduledDate and interactionType,
 * the NextScheduledInteractionService.toDto() mapping SHALL produce a NextScheduledDto
 * where scheduledAt equals scheduledDate.toString() (ISO-8601 format) and type equals
 * interactionType.name(). When the repository returns empty, the service SHALL return null.
 * <p>
 * <b>Validates: Requirements 2.1, 2.4, 2.5</b>
 */
@Tag("Feature: next-scheduled-interaction, Property 2: Service DTO mapping round-trip")
class NextScheduledDtoMappingPropertyTest {

	private static final LocalDate REFERENCE_DATE = LocalDate.of(2025, 6, 15);
	private static final Clock FIXED_CLOCK = Clock.fixed(
			REFERENCE_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant(),
			ZoneId.systemDefault());

	private NextScheduledInteractionService service;
	private ScheduledInteractionRepository repository;

	@BeforeProperty
	void setUp() {
		repository = Mockito.mock(ScheduledInteractionRepository.class);
		service = new NextScheduledInteractionServiceImpl(repository, FIXED_CLOCK);
	}

	// **Validates: Requirements 2.1, 2.4, 2.5**

	@Property(tries = 100)
	void dtoMappingShouldProduceCorrectScheduledAtAndType(
			@ForAll("futureDates") LocalDate scheduledDate,
			@ForAll("interactionTypes") InteractionType interactionType) {

		Long employeeId = 1L;

		ScheduledInteraction entity = new ScheduledInteraction();
		entity.setId(42L);
		entity.setScheduledDate(scheduledDate);
		entity.setInteractionType(interactionType);
		entity.setCompletionStatus(CompletionStatus.PENDING);

		Employee employee = new Employee();
		employee.setId(employeeId);
		entity.setEmployee(employee);

		when(repository.findNextPendingByEmployeeId(eq(employeeId), any(LocalDate.class)))
				.thenReturn(Optional.of(entity));

		NextScheduledDto result = service.getNextScheduled(employeeId);

		assertThat(result).isNotNull();
		assertThat(result.scheduledAt()).isEqualTo(scheduledDate.toString());
		assertThat(result.type()).isEqualTo(interactionType.name());
	}

	@Property(tries = 100)
	void shouldReturnNullWhenRepositoryReturnsEmpty(
			@ForAll("employeeIds") Long employeeId) {

		when(repository.findNextPendingByEmployeeId(eq(employeeId), any(LocalDate.class)))
				.thenReturn(Optional.empty());

		NextScheduledDto result = service.getNextScheduled(employeeId);

		assertThat(result).isNull();
	}

	@Provide
	Arbitrary<LocalDate> futureDates() {
		// Generate dates from 0 to 730 days after the reference date (on or after today)
		return Arbitraries.integers().between(0, 730)
				.map(REFERENCE_DATE::plusDays);
	}

	@Provide
	Arbitrary<InteractionType> interactionTypes() {
		return Arbitraries.of(InteractionType.values());
	}

	@Provide
	Arbitrary<Long> employeeIds() {
		return Arbitraries.longs().between(1L, 10000L);
	}
}
