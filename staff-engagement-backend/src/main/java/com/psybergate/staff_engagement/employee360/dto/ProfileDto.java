package com.psybergate.staff_engagement.employee360.dto;

public record ProfileDto(
	Long id,
	String name,
	String email,
	String jobTitle,
	String managerName
) {}
