package com.psybergate.staff_engagement.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateEmployeeRequest(
	@NotBlank(message = "Name is required")
	@Size(max = 255, message = "Name must be at most 255 characters")
	String name,

	@NotBlank(message = "Email is required")
	@Email(message = "Email must be a valid email address")
	@Size(max = 255, message = "Email must be at most 255 characters")
	String email,

	@Size(max = 255, message = "Job title must be at most 255 characters")
	String jobTitle,

	Long managerId
) {}
