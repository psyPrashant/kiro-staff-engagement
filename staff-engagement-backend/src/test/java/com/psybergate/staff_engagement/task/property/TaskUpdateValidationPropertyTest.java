package com.psybergate.staff_engagement.task.property;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.psybergate.staff_engagement.common.exception.GlobalExceptionHandler;
import com.psybergate.staff_engagement.task.dto.UpdateTaskRequest;
import com.psybergate.staff_engagement.task.service.TaskService;
import com.psybergate.staff_engagement.task.web.TaskController;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Controller-level property test for validation precedence using jqwik + standalone MockMvc.
 *
 * Feature: task-edit-delete-api
 */
class TaskUpdateValidationPropertyTest {

	@Mock
	private TaskService taskService;

	private MockMvc mockMvc;

	@BeforeProperty
	void setUp() {
		MockitoAnnotations.openMocks(this);
		TaskController controller = new TaskController(taskService);
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	/**
	 * Property 7: Bean validation failures take precedence over foreign-key resolution failures
	 *
	 * For any UpdateTaskRequest JSON body that is simultaneously bean-invalid (blank or
	 * oversized title) and FK-invalid (an unresolvable id for a FK field), PUT returns 400
	 * with a fieldErrors body and taskService.update is never invoked.
	 *
	 * Validates: Requirements 5.4
	 */
	@Property(tries = 100)
	@Tag("Feature: task-edit-delete-api, Property 7: Bean validation failures take precedence over foreign-key resolution failures")
	void beanValidationPrecedesForeignKeyResolution(
			@ForAll("invalidTitles") String title,
			@ForAll("unresolvableIds") long fkId) throws Exception {
		String requestBody = """
				{
					"title": "%s",
					"employeeId": %d
				}
				""".formatted(title, fkId);

		mockMvc.perform(put("/api/tasks/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors").exists())
				.andExpect(jsonPath("$.fieldErrors.title").exists());

		verify(taskService, never()).update(anyLong(), any(UpdateTaskRequest.class));
	}

	/**
	 * Generates bean-invalid titles: blank (empty) or oversized (256+ chars).
	 */
	@Provide
	Arbitrary<String> invalidTitles() {
		Arbitrary<String> blank = Arbitraries.just("");
		Arbitrary<String> oversized = Arbitraries.strings()
				.withCharRange('a', 'z')
				.ofMinLength(256)
				.ofMaxLength(500);
		return Arbitraries.oneOf(blank, oversized);
	}

	/**
	 * Generates ids that do not resolve to any record.
	 */
	@Provide
	Arbitrary<Long> unresolvableIds() {
		return Arbitraries.longs().between(100_000L, 1_000_000L);
	}
}
