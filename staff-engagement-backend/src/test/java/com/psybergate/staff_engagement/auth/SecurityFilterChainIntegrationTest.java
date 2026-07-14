package com.psybergate.staff_engagement.auth;

import com.psybergate.staff_engagement.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the SecurityFilterChain configuration.
 * Verifies that access rules are correctly enforced for protected and permitted endpoints.
 *
 * Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5
 */
@AutoConfigureMockMvc
class SecurityFilterChainIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void unauthenticatedAccessToProtectedEndpointReturns401WithJson() throws Exception {
		mockMvc.perform(get("/api/users")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
			.andExpect(jsonPath("$.error").value("Authentication required"));
	}

	@Test
	void authenticatedAccessToProtectedEndpointSucceeds() throws Exception {
		// Simulate an authenticated user via Spring Security Test support
		mockMvc.perform(get("/api/users")
				.with(user("alice.johnson@psybergate.com"))
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());
	}

	@Test
	void unauthenticatedAccessToAuthMeReturns401WithJson() throws Exception {
		// Confirms .requestMatchers("/api/**").authenticated() already covers
		// GET /api/auth/me — no explicit matcher is required for it.
		mockMvc.perform(get("/api/auth/me")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
			.andExpect(jsonPath("$.error").value("Authentication required"));
	}

	@Test
	void authenticatedAccessToAuthMeSucceeds() throws Exception {
		mockMvc.perform(get("/api/auth/me")
				.with(user("alice.johnson@psybergate.com"))
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());
	}

	@Test
	void unauthenticatedAccessToActuatorHealthSucceeds() throws Exception {
		mockMvc.perform(get("/actuator/health")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());
	}

	@Test
	void unauthenticatedAccessToLoginEndpointIsNotBlocked() throws Exception {
		// POST to login without auth should reach the controller (validation, not 401)
		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"\",\"password\":\"\"}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void unauthenticatedAccessToLogoutEndpointIsNotBlocked() throws Exception {
		// POST to logout without a valid session — handled by Spring Security's logout filter
		// Since logout is permitAll, it should not return 401 from the filter chain itself
		mockMvc.perform(post("/api/auth/logout"))
			.andExpect(status().isOk());
	}
}
