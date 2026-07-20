package com.psybergate.staff_engagement.engagement.dto;

import com.psybergate.staff_engagement.engagement.domain.EngagementStatus;
import java.time.LocalDate;

public record EngagementMatrixEntry(
	Long employeeId,
	String employeeName,
	String employeeEmail,
	Integer recency,
	int frequency,
	LocalDate lastInteractionDate,
	EngagementStatus engagementStatus,
	boolean followUpRequired
) {}
