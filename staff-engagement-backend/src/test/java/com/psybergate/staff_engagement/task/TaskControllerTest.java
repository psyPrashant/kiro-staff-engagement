package com.psybergate.staff_engagement.task;

import com.psybergate.staff_engagement.common.exception.GlobalExceptionHandler;
import com.psybergate.staff_engagement.task.dto.CreateTaskRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class TaskControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TaskRepository taskRepository;

	@MockitoBean
	private TaskService taskService;

	@Test
	void getAllTasks_returnsOkWithJsonArray() throws Exception {
		Task task1 = new Task();
		task1.setId(1L);
		task1.setTitle("Follow up on sprint goals");
		task1.setDescription("Review sprint backlog");
		task1.setStatus(TaskStatus.OPEN);
		task1.setDueDate(LocalDate.of(2025, 1, 15));
		task1.setCreatedAt(Instant.now());

		Task task2 = new Task();
		task2.setId(2L);
		task2.setTitle("Complete code review training");
		task2.setDescription("Finish the online module");
		task2.setStatus(TaskStatus.DONE);
		task2.setCreatedAt(Instant.now());

		org.mockito.Mockito.when(taskRepository.findAll()).thenReturn(List.of(task1, task2));

		mockMvc.perform(get("/api/tasks"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json"))
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].title").value("Follow up on sprint goals"))
				.andExpect(jsonPath("$[1].status").value("DONE"));
	}

	@Test
	void createTask_validRequest_returns201WithBody() throws Exception {
		Task savedTask = new Task();
		savedTask.setId(7L);
		savedTask.setTitle("Follow up on career development plan");
		savedTask.setStatus(TaskStatus.OPEN);
		savedTask.setCreatedAt(Instant.now());

		when(taskService.create(any(CreateTaskRequest.class))).thenReturn(savedTask);

		String requestBody = """
				{
					"title": "Follow up on career development plan"
				}
				""";

		mockMvc.perform(post("/api/tasks")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isCreated())
				.andExpect(content().contentType("application/json"))
				.andExpect(jsonPath("$.id").value(7))
				.andExpect(jsonPath("$.title").value("Follow up on career development plan"))
				.andExpect(jsonPath("$.status").value("OPEN"));
	}

	@Test
	void createTask_blankTitle_returns400WithFieldErrors() throws Exception {
		String requestBody = """
				{
					"title": ""
				}
				""";

		mockMvc.perform(post("/api/tasks")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Validation failed"))
				.andExpect(jsonPath("$.fieldErrors.title").exists());
	}

	@Test
	void createTask_titleTooLong_returns400WithFieldErrors() throws Exception {
		String longTitle = "a".repeat(256);
		String requestBody = """
				{
					"title": "%s"
				}
				""".formatted(longTitle);

		mockMvc.perform(post("/api/tasks")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Validation failed"))
				.andExpect(jsonPath("$.fieldErrors.title").exists());
	}

	@Test
	void createTask_serviceThrowsIllegalArgument_returns400WithMessage() throws Exception {
		when(taskService.create(any(CreateTaskRequest.class)))
				.thenThrow(new IllegalArgumentException("Interaction not found with id: 999"));

		String requestBody = """
				{
					"title": "Some task",
					"interactionId": 999
				}
				""";

		mockMvc.perform(post("/api/tasks")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Interaction not found with id: 999"));
	}
}
