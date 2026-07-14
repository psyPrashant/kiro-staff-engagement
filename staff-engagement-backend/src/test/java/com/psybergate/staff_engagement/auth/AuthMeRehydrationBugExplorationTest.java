package com.psybergate.staff_engagement.auth;

import com.psybergate.staff_engagement.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bug Condition Exploration Test — KSE-61 "[FE] Fix: page refresh logs the user out"
 *
 * Property 1: Expected Behavior - Reload With Valid Session Restores Authentication
 *
 * This is the SAME test written in task 1 to demonstrate the bug on unfixed code
 * (where it passed, asserting a 404 because {@code GET /api/auth/me} had no route
 * mapped). Task 3.1 added the {@code /me} mapping on {@link AuthController}, backed
 * by {@link CurrentUserResolver}. Re-running this test now confirms the fix: a
 * request carrying a valid authenticated session receives a 200 response with the
 * current user's id/name/email instead of a 404.
 *
 * Validates: Requirements 2.1, 2.2
 */
@AutoConfigureMockMvc
class AuthMeRehydrationBugExplorationTest extends BaseIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void getAuthMe_withValidAuthenticatedSession_returnsOkWithCurrentUser() throws Exception {
		// FIXED BEHAVIOR: with a valid authenticated session, GET /api/auth/me now
		// resolves the current user via CurrentUserResolver and returns 200 with
		// their id/name/email — no more 404, since the /me route is now mapped.
		mockMvc.perform(get("/api/auth/me")
				.with(user("alice.johnson@psybergate.com"))
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").isNumber())
			.andExpect(jsonPath("$.name").value("Alice Johnson"))
			.andExpect(jsonPath("$.email").value("alice.johnson@psybergate.com"));
	}
}
