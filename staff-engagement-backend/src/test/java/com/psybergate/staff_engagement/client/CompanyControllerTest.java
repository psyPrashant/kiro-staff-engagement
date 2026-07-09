package com.psybergate.staff_engagement.client;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CompanyController.class)
@WithMockUser
class CompanyControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CompanyRepository companyRepository;

	@Test
	void getCompanies_returnsOkWithJsonArray() throws Exception {
		Company company1 = new Company();
		company1.setId(1L);
		company1.setName("Acme Corp");
		company1.setCreatedAt(Instant.now());

		Company company2 = new Company();
		company2.setId(2L);
		company2.setName("Globex Inc");
		company2.setCreatedAt(Instant.now());

		org.mockito.Mockito.when(companyRepository.findAll()).thenReturn(List.of(company1, company2));

		mockMvc.perform(get("/api/companies"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json"))
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].name").value("Acme Corp"))
				.andExpect(jsonPath("$[1].name").value("Globex Inc"));
	}
}
