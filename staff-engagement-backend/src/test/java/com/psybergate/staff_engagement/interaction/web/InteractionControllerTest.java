package com.psybergate.staff_engagement.interaction.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.psybergate.staff_engagement.common.exception.GlobalExceptionHandler;
import com.psybergate.staff_engagement.employee.domain.Employee;
import com.psybergate.staff_engagement.interaction.domain.Interaction;
import com.psybergate.staff_engagement.interaction.domain.InteractionNotFoundException;
import com.psybergate.staff_engagement.interaction.service.InteractionService;
import com.psybergate.staff_engagement.interaction.domain.InteractionType;
import com.psybergate.staff_engagement.interaction.dto.CreateInteractionRequest;
import com.psybergate.staff_engagement.interaction.dto.UpdateInteractionRequest;
import com.psybergate.staff_engagement.interaction.service.InteractionService;
import com.psybergate.staff_engagement.user.domain.User;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InteractionController.class)
@Import(GlobalExceptionHandler.class)
@WithMockUser
class InteractionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private InteractionService interactionService;

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

		when(interactionService.listAll()).thenReturn(List.of(interaction1, interaction2));

		mockMvc.perform(get("/api/interactions"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json"))
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].type").value("CHECK_IN"))
				.andExpect(jsonPath("$[1].type").value("MENTORING"));
	}

	@Test
	void createInteraction_validRequest_returns201WithBody() throws Exception {
		User user = new User();
		user.setId(2L);
		user.setName("Alice Johnson");
		user.setEmail("alice@example.com");
		user.setCreatedAt(Instant.now());

		Employee employee = new Employee();
		employee.setId(1L);
		employee.setName("Jane Doe");
		employee.setEmail("jane@acme.com");
		employee.setJobTitle("Software Engineer");
		employee.setCreatedAt(Instant.now());

		Interaction savedInteraction = new Interaction();
		savedInteraction.setId(42L);
		savedInteraction.setEmployee(employee);
		savedInteraction.setConductedBy(user);
		savedInteraction.setLoggedBy(user);
		savedInteraction.setType(InteractionType.CHECK_IN);
		savedInteraction.setNotes("Weekly check-in discussion");
		savedInteraction.setOccurredAt(Instant.parse("2024-12-01T10:00:00Z"));
		savedInteraction.setCreatedAt(Instant.now());

		when(interactionService.create(any(CreateInteractionRequest.class))).thenReturn(savedInteraction);

		String requestBody = """
				{
					"employeeId": 1,
					"conductedByUserId": 2,
					"loggedByUserId": 2,
					"type": "CHECK_IN",
					"notes": "Weekly check-in discussion",
					"occurredAt": "2024-12-01T10:00:00Z"
				}
				""";

		mockMvc.perform(post("/api/interactions")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isCreated())
				.andExpect(content().contentType("application/json"))
				.andExpect(jsonPath("$.id").value(42))
				.andExpect(jsonPath("$.type").value("CHECK_IN"))
				.andExpect(jsonPath("$.notes").value("Weekly check-in discussion"))
				.andExpect(jsonPath("$.occurredAt").value("2024-12-01T10:00:00Z"))
				.andExpect(jsonPath("$.employee.id").value(1))
				.andExpect(jsonPath("$.conductedBy.id").value(2))
				.andExpect(jsonPath("$.loggedBy.id").value(2));
	}

	@Test
	void createInteraction_missingRequiredFields_returns400WithFieldErrors() throws Exception {
		String requestBody = """
				{
					"employeeId": null,
					"conductedByUserId": null,
					"loggedByUserId": null,
					"type": null,
					"notes": "",
					"occurredAt": null
				}
				""";

		mockMvc.perform(post("/api/interactions")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Validation failed"))
				.andExpect(jsonPath("$.fieldErrors.employeeId").exists())
				.andExpect(jsonPath("$.fieldErrors.notes").exists());
	}

	@Test
	void createInteraction_serviceThrowsIllegalArgument_returns400WithMessage() throws Exception {
		when(interactionService.create(any(CreateInteractionRequest.class)))
				.thenThrow(new IllegalArgumentException("Employee not found with id: 999"));

		String requestBody = """
				{
					"employeeId": 999,
					"conductedByUserId": 2,
					"loggedByUserId": 2,
					"type": "CHECK_IN",
					"notes": "Some notes",
					"occurredAt": "2024-12-01T10:00:00Z"
				}
				""";

		mockMvc.perform(post("/api/interactions")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Employee not found with id: 999"));
	}

	@Test
	void updateInteraction_validRequest_returns200WithBody() throws Exception {
		Interaction updated = new Interaction();
		updated.setId(7L);
		updated.setType(InteractionType.CATCH_UP);
		updated.setNotes("Updated notes");
		updated.setOccurredAt(Instant.parse("2025-01-10T09:00:00Z"));

		when(interactionService.update(org.mockito.ArgumentMatchers.eq(7L), any(UpdateInteractionRequest.class)))
				.thenReturn(updated);

		String requestBody = """
				{
					"type": "CATCH_UP",
					"notes": "Updated notes",
					"occurredAt": "2025-01-10T09:00:00Z"
				}
				""";

		mockMvc.perform(put("/api/interactions/7")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(7))
				.andExpect(jsonPath("$.type").value("CATCH_UP"))
				.andExpect(jsonPath("$.notes").value("Updated notes"));
	}

	@Test
	void updateInteraction_nonExistentId_returns404() throws Exception {
		when(interactionService.update(org.mockito.ArgumentMatchers.eq(999L), any(UpdateInteractionRequest.class)))
				.thenThrow(new InteractionNotFoundException(999L));

		String requestBody = """
				{
					"type": "CATCH_UP",
					"notes": "Updated notes",
					"occurredAt": "2025-01-10T09:00:00Z"
				}
				""";

		mockMvc.perform(put("/api/interactions/999")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Interaction not found with id: 999"));
	}

	@Test
	void updateInteraction_missingRequiredFields_returns400WithFieldErrors() throws Exception {
		String requestBody = """
				{
					"type": null,
					"notes": "",
					"occurredAt": null
				}
				""";

		mockMvc.perform(put("/api/interactions/7")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Validation failed"));
	}

	@Test
	void deleteInteraction_existingId_returns204() throws Exception {
		mockMvc.perform(delete("/api/interactions/7").with(csrf()))
				.andExpect(status().isNoContent());
	}

	@Test
	void deleteInteraction_nonExistentId_returns404() throws Exception {
		org.mockito.Mockito.doThrow(new InteractionNotFoundException(999L))
				.when(interactionService).delete(999L);

		mockMvc.perform(delete("/api/interactions/999").with(csrf()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Interaction not found with id: 999"));
	}
}
