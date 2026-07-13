package com.psybergate.staff_engagement.scheduling;

import com.psybergate.staff_engagement.interaction.InteractionType;

import java.time.Instant;
import java.time.LocalDate;

public record ScheduledInteractionResponse(
	Long id,
	Long employeeId,
	String employeeName,
	LocalDate scheduledDate,
	InteractionType interactionType,
	CompletionStatus completionStatus,
	String notes,
	boolean overdue,
	Instant createdAt
) {}
