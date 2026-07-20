package com.psybergate.staff_engagement.interaction.dto;

import com.psybergate.staff_engagement.interaction.domain.InteractionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record UpdateInteractionRequest(
	@NotNull InteractionType type,
	@NotBlank String notes,
	@NotNull Instant occurredAt,
	Long projectId
) {}
