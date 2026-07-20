package com.psybergate.staff_engagement.scheduling.dto;

import com.psybergate.staff_engagement.interaction.domain.InteractionType;

public record NextScheduledDto(
	String scheduledAt,  // ISO-8601 date string, e.g. "2025-02-15"
	String type          // InteractionType name, e.g. "CHECK_IN"
) {}
