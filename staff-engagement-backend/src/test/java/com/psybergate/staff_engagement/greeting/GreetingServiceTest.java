package com.psybergate.staff_engagement.greeting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GreetingServiceTest {

	@Mock
	private Clock clock;

	@InjectMocks
	private GreetingService greetingService;

	@Test
	void shouldReturnMorningGreetingBeforeNoon() {
		// Given: clock returns 9:00 AM
		Clock fixedClock = Clock.fixed(
				Instant.parse("2025-01-15T09:00:00Z"),
				ZoneId.of("UTC")
		);
		when(clock.instant()).thenReturn(fixedClock.instant());
		when(clock.getZone()).thenReturn(fixedClock.getZone());

		// When
		String result = greetingService.greet("Alice");

		// Then
		assertThat(result).isEqualTo("Good morning, Alice!");
		verify(clock).instant();
	}

	@Test
	void shouldReturnAfternoonGreetingAfterNoon() {
		// Given: clock returns 2:00 PM
		Clock fixedClock = Clock.fixed(
				Instant.parse("2025-01-15T14:00:00Z"),
				ZoneId.of("UTC")
		);
		when(clock.instant()).thenReturn(fixedClock.instant());
		when(clock.getZone()).thenReturn(fixedClock.getZone());

		// When
		String result = greetingService.greet("Bob");

		// Then
		assertThat(result).isEqualTo("Good afternoon, Bob!");
		verify(clock).getZone();
	}

	@Test
	void shouldReturnEveningGreetingAfterSixPM() {
		// Given: clock returns 8:00 PM
		Clock fixedClock = Clock.fixed(
				Instant.parse("2025-01-15T20:00:00Z"),
				ZoneId.of("UTC")
		);
		when(clock.instant()).thenReturn(fixedClock.instant());
		when(clock.getZone()).thenReturn(fixedClock.getZone());

		// When
		String result = greetingService.greet("Charlie");

		// Then
		assertThat(result).isEqualTo("Good evening, Charlie!");
		verify(clock).instant();
	}
}
