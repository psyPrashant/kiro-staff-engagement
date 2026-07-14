package com.psybergate.staff_engagement.auth;

import com.psybergate.staff_engagement.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the current-user rehydration endpoint (GET /api/auth/me).
 *
 * Validates: Requirements 2.2, 3.3
 */
@AutoConfigureMockMvc
class AuthMeIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void authenticatedSession_returnsOkWithCurrentUserDetails() throws Exception {
		// First, login to establish a real authenticated session backed by the DB
		String loginBody = """
				{"email": "alice.johnson@psybergate.com", "password": "Password1"}
				""";

		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginBody))
				.andExpect(status().isOk())
				.andReturn();

		MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

		// Now call /me using that session and assert the actual user's data is returned
		mockMvc.perform(get("/api/auth/me")
						.session(session)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.name").value("Alice Johnson"))
				.andExpect(jsonPath("$.email").value("alice.johnson@psybergate.com"));
	}

	@Test
	void noAuthenticatedSession_returnsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/auth/me")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized());
	}
}
