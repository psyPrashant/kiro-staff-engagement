package com.psybergate.staff_engagement.interaction.property;

import static org.assertj.core.api.Assertions.assertThat;

import com.psybergate.staff_engagement.interaction.domain.Interaction;
import com.psybergate.staff_engagement.interaction.domain.InteractionType;
import com.psybergate.staff_engagement.interaction.dto.CreateInteractionRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.jqwik.api.*;

/**
 * Property-based tests for Interaction bean validation using jqwik.
 *
 * Validates: Requirements 1.2, 1.8
 */
class InteractionPropertyTest {

	private static final Validator validator;

	static {
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
	}

	private static final List<String> REQUIRED_FIELDS = List.of(
			"employeeId", "conductedByUserId", "loggedByUserId", "type", "notes", "occurredAt"
	);

	/**
	 * Property 2: Interaction bean validation rejects invalid requests
	 *
	 * For any CreateInteractionRequest where one or more required fields
	 * (employeeId, conductedByUserId, loggedByUserId, type, notes, occurredAt)
	 * are null or blank, validation SHALL produce a constraint violation for that field.
	 *
	 * Validates: Requirements 1.2, 1.8
	 */
	@Property(tries = 100)
	@Tag("Feature: interaction-task-write-api, Property 2: Interaction bean validation rejects invalid requests")
	void beanValidationRejectsInvalidRequests(
			@ForAll("invalidInteractionRequests") InvalidRequest invalidRequest) {

		Set<ConstraintViolation<CreateInteractionRequest>> violations =
				validator.validate(invalidRequest.request());

		Set<String> violatedFields = violations.stream()
				.map(v -> v.getPropertyPath().toString())
				.collect(Collectors.toSet());

		assertThat(violations).isNotEmpty();
		for (String expectedField : invalidRequest.invalidFields()) {
			assertThat(violatedFields).contains(expectedField);
		}
	}

	@Provide
	Arbitrary<InvalidRequest> invalidInteractionRequests() {
		// Generate a non-empty subset of required fields to nullify
		Arbitrary<Set<String>> fieldsToNullify = Arbitraries.subsetOf(REQUIRED_FIELDS)
				.filter(s -> !s.isEmpty());

		return fieldsToNullify.map(nullifiedFields -> {
			Long employeeId = nullifiedFields.contains("employeeId") ? null : 1L;
			Long conductedByUserId = nullifiedFields.contains("conductedByUserId") ? null : 2L;
			Long loggedByUserId = nullifiedFields.contains("loggedByUserId") ? null : 3L;
			InteractionType type = nullifiedFields.contains("type") ? null : InteractionType.CHECK_IN;
			String notes = nullifiedFields.contains("notes") ? blankNotes() : "Valid notes content";
			Instant occurredAt = nullifiedFields.contains("occurredAt") ? null : Instant.now();

			CreateInteractionRequest request = new CreateInteractionRequest(
					employeeId, conductedByUserId, loggedByUserId,
					type, notes, occurredAt, null
			);

			return new InvalidRequest(request, List.copyOf(nullifiedFields));
		});
	}

	private String blankNotes() {
		// Randomly return null, empty, or whitespace-only string
		int choice = (int) (Math.random() * 3);
		return switch (choice) {
			case 0 -> null;
			case 1 -> "";
			default -> "   ";
		};
	}

	record InvalidRequest(CreateInteractionRequest request, List<String> invalidFields) {}
}
