package com.psybergate.staff_engagement.employee;

import com.psybergate.staff_engagement.scheduling.NextScheduledDto;
import com.psybergate.staff_engagement.scheduling.NextScheduledInteractionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmployeeController.class)
@WithMockUser
class EmployeeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private EmployeeRepository employeeRepository;

	@MockitoBean
	private NextScheduledInteractionService nextScheduledService;

	@MockitoBean
	private EmployeeService employeeService;

	@Test
	void getAllEmployees_returnsOkWithJsonArray() throws Exception {
		Employee emp1 = new Employee();
		emp1.setId(1L);
		emp1.setName("Jane Doe");
		emp1.setEmail("jane@acme.com");
		emp1.setJobTitle("Software Engineer");
		emp1.setCreatedAt(Instant.now());

		Employee emp2 = new Employee();
		emp2.setId(2L);
		emp2.setName("John Wick");
		emp2.setEmail("john@acme.com");
		emp2.setJobTitle("Team Lead");
		emp2.setCreatedAt(Instant.now());

		when(employeeRepository.findAll()).thenReturn(List.of(emp1, emp2));
		when(nextScheduledService.getNextScheduledBatch(anyList()))
				.thenReturn(Map.of(1L, new NextScheduledDto("2025-03-15", "CHECK_IN")));

		mockMvc.perform(get("/api/employees"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json"))
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].name").value("Jane Doe"))
				.andExpect(jsonPath("$[0].nextScheduled.scheduledAt").value("2025-03-15"))
				.andExpect(jsonPath("$[0].nextScheduled.type").value("CHECK_IN"))
				.andExpect(jsonPath("$[1].name").value("John Wick"))
				.andExpect(jsonPath("$[1].nextScheduled").doesNotExist());
	}
}
