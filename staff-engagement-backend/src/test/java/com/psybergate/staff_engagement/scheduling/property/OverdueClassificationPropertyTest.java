package com.psybergate.staff_engagement.scheduling.property;

import static org.assertj.core.api.Assertions.assertThat;

import com.psybergate.staff_engagement.scheduling.domain.CompletionStatus;
import com.psybergate.staff_engagement.scheduling.service.SchedulingService;
import com.psybergate.staff_engagement.scheduling.service.SchedulingServiceImpl;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import net.jqwik.api.*;

/**
 * Property-based tests for overdue classification logic.
 *
 * Feature: interaction-scheduling, Property 4: Overdue Classification (Backend)
 * **Validates: Requirements 5.1, 5.2, 5.6, 10.6**
 */
@Tag("Feature: interaction-scheduling, Property 4: Overdue Classification")
class OverdueClassificationPropertyTest {

	@Property(tries = 200)
	void isOverdueReturnsTrueIffScheduledDateBeforeReferenceDateAndStatusPending(
			@ForAll("scheduledDates") LocalDate scheduledDate,
			@ForAll("referenceDates") LocalDate referenceDate,
			@ForAll CompletionStatus status) {

		Clock fixedClock = Clock.fixed(
				referenceDate.atStartOfDay(ZoneId.systemDefault()).toInstant(),
				ZoneId.systemDefault());

		SchedulingService service = createServiceWithClock(fixedClock);

		boolean result = service.isOverdue(scheduledDate, status);

		boolean expected = scheduledDate.isBefore(referenceDate) && status == CompletionStatus.PENDING;
		assertThat(result).isEqualTo(expected);
	}

	@Provide
	Arbitrary<LocalDate> scheduledDates() {
		return Arbitraries.integers()
				.between(-365, 365)
				.map(offset -> LocalDate.of(2025, 6, 15).plusDays(offset));
	}

	@Provide
	Arbitrary<LocalDate> referenceDates() {
		return Arbitraries.integers()
				.between(-365, 365)
				.map(offset -> LocalDate.of(2025, 6, 15).plusDays(offset));
	}

	private SchedulingService createServiceWithClock(Clock clock) {
		return new SchedulingServiceImpl(null, null, null, clock);
	}
}
