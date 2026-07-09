package com.psybergate.staff_engagement.task;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TaskRepository taskRepository;

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
}
