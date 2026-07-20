package com.psybergate.staff_engagement.auth.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.psybergate.staff_engagement.support.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for the session-rehydration endpoint (GET /api/auth/me),
 * used by the frontend to restore currentUser after a hard page reload.
 */
@AutoConfigureMockMvc
class AuthMeIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void withValidSession_returnsOkWithUserDetails() throws Exception {
		String loginBody = """
				{"email": "alice.johnson@psybergate.com", "password": "Password1"}
				""";

		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginBody))
				.andExpect(status().isOk())
				.andReturn();

		MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

		mockMvc.perform(get("/api/auth/me")
						.session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.name").value("Alice Johnson"))
				.andExpect(jsonPath("$.email").value("alice.johnson@psybergate.com"));
	}

	@Test
	void withoutSession_returnsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/auth/me"))
				.andExpect(status().isUnauthorized());
	}
}
