package com.psybergate.staff_engagement.client;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProjectController.class)
class ProjectControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProjectRepository projectRepository;

	@Test
	void getProjects_returnsOkWithJsonArray() throws Exception {
		Company company = new Company();
		company.setId(1L);
		company.setName("Acme Corp");
		company.setCreatedAt(Instant.now());

		Project project1 = new Project();
		project1.setId(1L);
		project1.setName("Project Alpha");
		project1.setCompany(company);
		project1.setCreatedAt(Instant.now());

		Project project2 = new Project();
		project2.setId(2L);
		project2.setName("Project Beta");
		project2.setCompany(company);
		project2.setCreatedAt(Instant.now());

		org.mockito.Mockito.when(projectRepository.findAll()).thenReturn(List.of(project1, project2));

		mockMvc.perform(get("/api/projects"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json"))
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].name").value("Project Alpha"))
				.andExpect(jsonPath("$[1].name").value("Project Beta"));
	}
}
