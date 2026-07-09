package com.psybergate.staff_engagement.interaction;

import com.psybergate.staff_engagement.employee.Employee;
import com.psybergate.staff_engagement.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InteractionController.class)
class InteractionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private InteractionRepository interactionRepository;

	@Test
	void getAllInteractions_returnsOkWithJsonArray() throws Exception {
		User user = new User();
		user.setId(1L);
		user.setName("Alice Johnson");
		user.setEmail("alice@example.com");
		user.setCreatedAt(Instant.now());

		Employee employee = new Employee();
		employee.setId(1L);
		employee.setName("Jane Doe");
		employee.setEmail("jane@acme.com");
		employee.setJobTitle("Software Engineer");
		employee.setCreatedAt(Instant.now());

		Interaction interaction1 = new Interaction();
		interaction1.setId(1L);
		interaction1.setEmployee(employee);
		interaction1.setConductedBy(user);
		interaction1.setLoggedBy(user);
		interaction1.setType(InteractionType.CHECK_IN);
		interaction1.setNotes("Weekly check-in");
		interaction1.setOccurredAt(Instant.now());
		interaction1.setCreatedAt(Instant.now());

		Interaction interaction2 = new Interaction();
		interaction2.setId(2L);
		interaction2.setEmployee(employee);
		interaction2.setConductedBy(user);
		interaction2.setLoggedBy(user);
		interaction2.setType(InteractionType.MENTORING);
		interaction2.setNotes("Mentoring session");
		interaction2.setOccurredAt(Instant.now());
		interaction2.setCreatedAt(Instant.now());

		org.mockito.Mockito.when(interactionRepository.findAll()).thenReturn(List.of(interaction1, interaction2));

		mockMvc.perform(get("/api/interactions"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json"))
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].type").value("CHECK_IN"))
				.andExpect(jsonPath("$[1].type").value("MENTORING"));
	}
}
