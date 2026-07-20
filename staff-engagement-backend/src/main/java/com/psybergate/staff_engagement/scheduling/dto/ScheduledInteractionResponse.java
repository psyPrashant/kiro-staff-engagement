package com.psybergate.staff_engagement.scheduling.dto;

import com.psybergate.staff_engagement.interaction.domain.InteractionType;
import com.psybergate.staff_engagement.scheduling.domain.CompletionStatus;
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
