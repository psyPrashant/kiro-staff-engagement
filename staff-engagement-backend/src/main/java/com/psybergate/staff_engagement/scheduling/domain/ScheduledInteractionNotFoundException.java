package com.psybergate.staff_engagement.scheduling.domain;

public class ScheduledInteractionNotFoundException extends RuntimeException {

	public ScheduledInteractionNotFoundException(Long id) {
		super("Scheduled interaction not found with id: " + id);
	}
}
