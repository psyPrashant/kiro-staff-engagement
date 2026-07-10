package com.psybergate.staff_engagement.engagement;

import com.psybergate.staff_engagement.common.exception.GlobalExceptionHandler;
import com.psybergate.staff_engagement.engagement.dto.EngagementMatrixEntry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EngagementController.class)
@Import(GlobalExceptionHandler.class)
class EngagementControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private EngagementService engagementService;

	@Test
	@WithMockUser
	void getMatrix_validRequest_returns200WithJsonArray() throws Exception {
		when(engagementService.computeMatrix(null, null, null))
				.thenReturn(List.of(new EngagementMatrixEntry(
						1L, "Alice", "alice@test.com", 5, 3,
						LocalDate.of(2025, 1, 10), EngagementStatus.ON_TRACK, false)));

		mockMvc.perform(get("/api/engagement/matrix"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json"))
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$[0].employeeName").value("Alice"))
				.andExpect(jsonPath("$[0].recency").value(5))
				.andExpect(jsonPath("$[0].frequency").value(3))
				.andExpect(jsonPath("$[0].engagementStatus").value("ON_TRACK"))
				.andExpect(jsonPath("$[0].followUpRequired").value(false));
	}

	@Test
	@WithMockUser
	void getMatrix_invalidStatus_returns400WithValidOptions() throws Exception {
		mockMvc.perform(get("/api/engagement/matrix").param("status", "INVALID"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(containsString("Valid options are: OVERDUE, AT_RISK, ON_TRACK")));
	}

	@Test
	@WithMockUser
	void getMatrix_invalidSort_returns400WithSupportedOptions() throws Exception {
		mockMvc.perform(get("/api/engagement/matrix").param("sort", "name"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(containsString("Supported options are: recency")));
	}

	@Test
	@WithMockUser
	void getMatrix_dataAccessException_returns500WithGenericMessage() throws Exception {
		when(engagementService.computeMatrix(any(), any(), any()))
				.thenThrow(new QueryTimeoutException("timeout"));

		mockMvc.perform(get("/api/engagement/matrix"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.message").value("Unable to compute engagement matrix due to a data access failure"));
	}
}
