package com.psybergate.staff_engagement.greeting;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class GreetingService {

	private final Clock clock;

	public GreetingService(Clock clock) {
		this.clock = clock;
	}

	public String greet(String name) {
		LocalDateTime now = LocalDateTime.now(clock);
		int hour = now.getHour();

		String timeOfDay;
		if (hour < 12) {
			timeOfDay = "Good morning";
		} else if (hour < 18) {
			timeOfDay = "Good afternoon";
		} else {
			timeOfDay = "Good evening";
		}

		return timeOfDay + ", " + name + "!";
	}
}
