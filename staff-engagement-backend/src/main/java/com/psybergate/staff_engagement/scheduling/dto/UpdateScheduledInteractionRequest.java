package com.psybergate.staff_engagement.scheduling.dto;

import com.psybergate.staff_engagement.scheduling.domain.CompletionStatus;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateScheduledInteractionRequest(
	CompletionStatus completionStatus,
	LocalDate scheduledDate,
	@Size(max = 2000) String notes
) {}
