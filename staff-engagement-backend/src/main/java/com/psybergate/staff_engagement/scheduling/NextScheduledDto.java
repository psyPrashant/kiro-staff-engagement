package com.psybergate.staff_engagement.scheduling;

public record NextScheduledDto(
	String scheduledAt,  // ISO-8601 date string, e.g. "2025-02-15"
	String type          // InteractionType name, e.g. "CHECK_IN"
) {}
