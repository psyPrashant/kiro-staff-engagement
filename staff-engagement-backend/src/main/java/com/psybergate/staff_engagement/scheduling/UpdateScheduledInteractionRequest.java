package com.psybergate.staff_engagement.scheduling;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateScheduledInteractionRequest(
	CompletionStatus completionStatus,
	LocalDate scheduledDate,
	@Size(max = 2000) String notes
) {}
