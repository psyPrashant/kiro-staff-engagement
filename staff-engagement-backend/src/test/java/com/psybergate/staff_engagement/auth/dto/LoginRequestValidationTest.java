package com.psybergate.staff_engagement.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginRequestValidationTest {

	private Validator validator;

	@BeforeEach
	void setUp() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	@Test
	void validRequest_noViolations() {
		LoginRequest request = new LoginRequest("user@example.com", "password123");

		Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

		assertThat(violations).isEmpty();
	}

	@Test
	void blankEmail_rejected() {
		LoginRequest request = new LoginRequest("", "password123");

		Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

		assertThat(violations).isNotEmpty();
		assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
	}

	@Test
	void blankPassword_rejected() {
		LoginRequest request = new LoginRequest("user@example.com", "");

		Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

		assertThat(violations).isNotEmpty();
		assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
	}

	@Test
	void nullEmail_rejected() {
		LoginRequest request = new LoginRequest(null, "password123");

		Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

		assertThat(violations).isNotEmpty();
		assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
	}

	@Test
	void nullPassword_rejected() {
		LoginRequest request = new LoginRequest("user@example.com", null);

		Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

		assertThat(violations).isNotEmpty();
		assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
	}

	@Test
	void oversizedEmail_rejected() {
		String longEmail = "a".repeat(256);
		LoginRequest request = new LoginRequest(longEmail, "password123");

		Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

		assertThat(violations).isNotEmpty();
		assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
	}

	@Test
	void oversizedPassword_rejected() {
		String longPassword = "p".repeat(129);
		LoginRequest request = new LoginRequest("user@example.com", longPassword);

		Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

		assertThat(violations).isNotEmpty();
		assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
	}
}
