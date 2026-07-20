package com.psybergate.staff_engagement.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
	@NotBlank(message = "Email is required")
	@Size(max = 255, message = "Email must not exceed 255 characters")
	String email,

	@NotBlank(message = "Password is required")
	@Size(max = 128, message = "Password must not exceed 128 characters")
	String password
) {}
