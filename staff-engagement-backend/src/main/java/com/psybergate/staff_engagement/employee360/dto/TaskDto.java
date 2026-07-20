package com.psybergate.staff_engagement.employee360.dto;

import java.time.LocalDate;

public record TaskDto(
	Long id,
	String title,
	LocalDate dueDate,
	String assignedUserName
) {}
