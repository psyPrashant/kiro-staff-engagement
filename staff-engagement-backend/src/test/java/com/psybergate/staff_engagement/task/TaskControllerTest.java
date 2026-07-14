package com.psybergate.staff_engagement.task;

import com.psybergate.staff_engagement.common.exception.GlobalExceptionHandler;
import com.psybergate.staff_engagement.employee.Employee;
import com.psybergate.staff_engagement.task.dto.CreateTaskRequest;
import com.psybergate.staff_engagement.task.dto.TaskResponse;
import com.psybergate.staff_engagement.task.dto.UpdateTaskRequest;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

	@Test
	void createTask_withEmployeeId_returns201WithEmployeeDetails() throws Exception {
		Employee employee = new Employee();
		employee.setId(5L);
		employee.setName("John Doe");
		employee.setEmail("john.doe@example.com");

		Task savedTask = new Task();
		savedTask.setId(10L);
		savedTask.setTitle("Review employee performance");
		savedTask.setStatus(TaskStatus.OPEN);
		savedTask.setEmployee(employee);
		savedTask.setCreatedAt(Instant.now());

		when(taskService.create(any(CreateTaskRequest.class))).thenReturn(savedTask);

		String requestBody = """
				{
					"title": "Review employee performance",
					"employeeId": 5
				}
				""";

		mockMvc.perform(post("/api/tasks")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isCreated())
				.andExpect(content().contentType("application/json"))
				.andExpect(jsonPath("$.id").value(10))
				.andExpect(jsonPath("$.title").value("Review employee performance"))
				.andExpect(jsonPath("$.employeeId").value(5))
				.andExpect(jsonPath("$.employeeName").value("John Doe"));
	}

	@Test
	void createTask_withInvalidEmployeeId_returns400() throws Exception {
		when(taskService.create(any(CreateTaskRequest.class)))
				.thenThrow(new IllegalArgumentException("Employee not found with id: 999"));

		String requestBody = """
				{
					"title": "Some task",
					"employeeId": 999
				}
				""";

		mockMvc.perform(post("/api/tasks")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Employee not found with id: 999"));
	}

	@Test
	void getAllTasks_returnsTasksWithEmployeeFieldsPopulatedAndNull() throws Exception {
		Employee employee = new Employee();
		employee.setId(3L);
		employee.setName("Jane Smith");
		employee.setEmail("jane.smith@example.com");

		Task taskWithEmployee = new Task();
		taskWithEmployee.setId(1L);
		taskWithEmployee.setTitle("Task with employee");
		taskWithEmployee.setStatus(TaskStatus.OPEN);
		taskWithEmployee.setEmployee(employee);
		taskWithEmployee.setCreatedAt(Instant.now());

		Task taskWithoutEmployee = new Task();
		taskWithoutEmployee.setId(2L);
		taskWithoutEmployee.setTitle("Task without employee");
		taskWithoutEmployee.setStatus(TaskStatus.OPEN);
		taskWithoutEmployee.setCreatedAt(Instant.now());

		when(taskRepository.findAll()).thenReturn(List.of(taskWithEmployee, taskWithoutEmployee));

		mockMvc.perform(get("/api/tasks"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].employeeId").value(3))
				.andExpect(jsonPath("$[0].employeeName").value("Jane Smith"))
				.andExpect(jsonPath("$[1].employeeId").doesNotExist())
				.andExpect(jsonPath("$[1].employeeName").doesNotExist());
	}

	// --- PUT /api/tasks/{id} ---

	@Test
	void updateTask_validRequest_returns200WithBody() throws Exception {
		Task updatedTask = new Task();
		updatedTask.setId(7L);
		updatedTask.setTitle("Updated title");
		updatedTask.setStatus(TaskStatus.DONE);
		updatedTask.setCreatedAt(Instant.now());

		when(taskService.update(eq(7L), any(UpdateTaskRequest.class))).thenReturn(updatedTask);

		String requestBody = """
				{
					"title": "Updated title",
					"status": "DONE"
				}
				""";

		mockMvc.perform(put("/api/tasks/7")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json"))
				.andExpect(jsonPath("$.id").value(7))
				.andExpect(jsonPath("$.title").value("Updated title"))
				.andExpect(jsonPath("$.status").value("DONE"));
	}

	@Test
	void updateTask_blankTitle_returns400WithFieldErrors() throws Exception {
		String requestBody = """
				{
					"title": ""
				}
				""";

		mockMvc.perform(put("/api/tasks/7")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Validation failed"))
				.andExpect(jsonPath("$.fieldErrors.title").exists());
	}

	@Test
	void updateTask_taskNotFound_returns404WithMessage() throws Exception {
		when(taskService.update(eq(999L), any(UpdateTaskRequest.class)))
				.thenThrow(new TaskNotFoundException(999L));

		String requestBody = """
				{
					"title": "Updated title"
				}
				""";

		mockMvc.perform(put("/api/tasks/999")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Task not found with id: 999"));
	}

	@Test
	void updateTask_invalidStatusValue_returns400() throws Exception {
		String requestBody = """
				{
					"title": "Updated title",
					"status": "CANCELLED"
				}
				""";

		mockMvc.perform(put("/api/tasks/7")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isBadRequest());
	}

	// --- DELETE /api/tasks/{id} ---

	@Test
	void deleteTask_existingTask_returns204NoBody() throws Exception {
		doNothing().when(taskService).delete(7L);

		mockMvc.perform(delete("/api/tasks/7").with(csrf()))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		verify(taskService).delete(7L);
	}

	@Test
	void deleteTask_taskNotFound_returns404WithMessage() throws Exception {
		doThrow(new TaskNotFoundException(999L)).when(taskService).delete(anyLong());

		mockMvc.perform(delete("/api/tasks/999").with(csrf()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Task not found with id: 999"));
	}
}
