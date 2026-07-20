package com.psybergate.staff_engagement.task.property;

import static org.assertj.core.api.Assertions.assertThat;

import com.psybergate.staff_engagement.task.domain.Task;
import com.psybergate.staff_engagement.task.dto.CreateTaskRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDate;
import java.util.Set;
import net.jqwik.api.*;

/**
 * Property-based tests for Task bean validation using jqwik.
 *
 * Validates: Requirements 2.2, 2.7
 */
class TaskPropertyTest {

	private final Validator validator;

	TaskPropertyTest() {
		try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
			this.validator = factory.getValidator();
		}
	}

	/**
	 * Property 4: Task bean validation rejects invalid requests
	 *
	 * For any CreateTaskRequest where the title is blank or exceeds 255 characters,
	 * submitting it SHALL return HTTP 400 with a response body containing a fieldErrors
	 * map that includes "title" as a key.
	 *
	 * Validates: Requirements 2.2, 2.7
	 */
	@Property(tries = 100)
	@Tag("Feature: interaction-task-write-api, Property 4: Task bean validation rejects invalid requests")
	void blankTitleProducesValidationViolation(@ForAll("blankTitles") String title) {
		CreateTaskRequest request = new CreateTaskRequest(
				title,
				"Some valid description",
				null,
				null,
				LocalDate.now(),
				null
		);

		Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);

		assertThat(violations)
				.isNotEmpty()
				.anyMatch(v -> v.getPropertyPath().toString().equals("title"));
	}

	@Property(tries = 100)
	@Tag("Feature: interaction-task-write-api, Property 4: Task bean validation rejects invalid requests")
	void oversizedTitleProducesValidationViolation(@ForAll("oversizedTitles") String title) {
		CreateTaskRequest request = new CreateTaskRequest(
				title,
				"Some valid description",
				null,
				null,
				LocalDate.now(),
				null
		);

		Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);

		assertThat(violations)
				.isNotEmpty()
				.anyMatch(v -> v.getPropertyPath().toString().equals("title"));
	}

	/**
	 * Generates blank titles: empty strings, whitespace-only strings of various lengths.
	 */
	@Provide
	Arbitrary<String> blankTitles() {
		Arbitrary<String> emptyString = Arbitraries.just("");
		Arbitrary<String> whitespaceOnly = Arbitraries.integers().between(1, 50)
				.map(length -> " ".repeat(length));
		Arbitrary<String> tabsAndSpaces = Arbitraries.integers().between(1, 30)
				.flatMap(length -> Arbitraries.strings()
						.withChars(' ', '\t', '\n', '\r')
						.ofLength(length));

		return Arbitraries.oneOf(emptyString, whitespaceOnly, tabsAndSpaces);
	}

	/**
	 * Generates oversized titles: strings with length > 255 characters.
	 */
	@Provide
	Arbitrary<String> oversizedTitles() {
		return Arbitraries.strings()
				.withCharRange('a', 'z')
				.ofMinLength(256)
				.ofMaxLength(1000);
	}
}
