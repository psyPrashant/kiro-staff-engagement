package com.psybergate.staff_engagement.employee360;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(Employee360Controller.class)
class Employee360ControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private Employee360Service employee360Service;

	@Test
	@WithMockUser
	void getEmployee360_validId_returnsOkWithResponse() throws Exception {
		Employee360Response response = new Employee360Response(
			new ProfileDto(1L, "John Doe", "john@example.com", "Engineer", "Jane Manager"),
			List.of(new InteractionDto(10L, "CHECK_IN", Instant.parse("2024-12-15T10:00:00Z"), "Jane", "Notes", null)),
			List.of(new TaskDto(20L, "Task title", LocalDate.of(2025, 6, 15), "John"))
		);

		when(employee360Service.getEmployee360(1L)).thenReturn(response);

		mockMvc.perform(get("/api/employees/1/360"))
			.andExpect(status().isOk())
			.andExpect(content().contentType("application/json"))
			.andExpect(jsonPath("$.profile.id").value(1))
			.andExpect(jsonPath("$.profile.name").value("John Doe"))
			.andExpect(jsonPath("$.profile.email").value("john@example.com"))
			.andExpect(jsonPath("$.profile.jobTitle").value("Engineer"))
			.andExpect(jsonPath("$.profile.managerName").value("Jane Manager"))
			.andExpect(jsonPath("$.interactions").isArray())
			.andExpect(jsonPath("$.interactions.length()").value(1))
			.andExpect(jsonPath("$.interactions[0].id").value(10))
			.andExpect(jsonPath("$.interactions[0].type").value("CHECK_IN"))
			.andExpect(jsonPath("$.interactions[0].conductedByName").value("Jane"))
			.andExpect(jsonPath("$.openTasks").isArray())
			.andExpect(jsonPath("$.openTasks.length()").value(1))
			.andExpect(jsonPath("$.openTasks[0].id").value(20))
			.andExpect(jsonPath("$.openTasks[0].title").value("Task title"));
	}

	@Test
	@WithMockUser
	void getEmployee360_nonExistentId_returns404() throws Exception {
		when(employee360Service.getEmployee360(999L))
			.thenThrow(new Employee360NotFoundException(999L));

		mockMvc.perform(get("/api/employees/999/360"))
			.andExpect(status().isNotFound());
	}

	@Test
	@WithMockUser
	void getEmployee360_nonNumericId_returns400() throws Exception {
		mockMvc.perform(get("/api/employees/abc/360"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void getEmployee360_unauthenticated_returns401() throws Exception {
		mockMvc.perform(get("/api/employees/1/360"))
			.andExpect(status().isUnauthorized());
	}
}
