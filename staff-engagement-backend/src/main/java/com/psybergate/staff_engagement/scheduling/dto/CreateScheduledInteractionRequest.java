package com.psybergate.staff_engagement.scheduling.dto;

import com.psybergate.staff_engagement.interaction.domain.InteractionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateScheduledInteractionRequest(
	@NotNull Long employeeId,
	@NotNull LocalDate scheduledDate,
	@NotNull InteractionType interactionType,
	@Size(max = 2000) String notes
) {}
