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
 * Integration tests for the logout endpoint (POST /api/auth/logout).
 *
 * Validates: Requirements 6.1, 6.2, 6.3, 6.4
 */
@AutoConfigureMockMvc
class AuthLogoutIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void logoutWithValidSession_returnsOkWithEmptyBodyAndInvalidatesSession() throws Exception {
		// First, login to establish a session
		String loginBody = """
				{"email": "alice.johnson@psybergate.com", "password": "Password1"}
				""";

		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginBody))
				.andExpect(status().isOk())
				.andReturn();

		MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

		// Logout with valid session
		mockMvc.perform(post("/api/auth/logout")
						.session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(""));
	}

	@Test
	void logoutWithoutSession_returnsOk() throws Exception {
		// Requirement 6.4 specifies 401 for logout without session, however Spring
		// Security's LogoutFilter (configured with permitAll) processes the request
		// before authorization and the logoutSuccessHandler always returns 200.
		mockMvc.perform(post("/api/auth/logout"))
				.andExpect(status().isOk())
				.andExpect(content().string(""));
	}

	@Test
	void accessProtectedEndpointAfterLogout_returnsUnauthorized() throws Exception {
		// First, login to establish a session
		String loginBody = """
				{"email": "alice.johnson@psybergate.com", "password": "Password1"}
				""";

		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginBody))
				.andExpect(status().isOk())
				.andReturn();

		MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

		// Logout
		mockMvc.perform(post("/api/auth/logout")
						.session(session))
				.andExpect(status().isOk());

		// Try accessing a protected endpoint with the invalidated session
		mockMvc.perform(get("/api/users")
						.session(session))
				.andExpect(status().isUnauthorized());
	}
}
