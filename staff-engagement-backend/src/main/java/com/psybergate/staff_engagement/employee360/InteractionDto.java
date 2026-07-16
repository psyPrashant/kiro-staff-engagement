package com.psybergate.staff_engagement.employee360;

import java.time.Instant;

public record InteractionDto(
	Long id,
	String type,
	Instant occurredAt,
	String conductedByName,
	String notes,
	ProjectContextDto projectContext,
	Long projectId
) {}
