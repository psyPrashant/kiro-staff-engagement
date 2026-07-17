package com.psybergate.staff_engagement.client;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProjectController.class)
@WithMockUser
class ProjectControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProjectRepository projectRepository;

	@MockitoBean
	private ClientService clientService;

	@Test
	void getProjects_withCompanyId_returnsFilteredProjects() throws Exception {
		Company company = new Company();
		company.setId(1L);
		company.setName("Acme Corp");
		company.setCreatedAt(Instant.now());

		Project project1 = new Project();
		project1.setId(1L);
		project1.setName("Project Alpha");
		project1.setCompany(company);
		project1.setCreatedAt(Instant.now());

		org.mockito.Mockito.when(projectRepository.findByCompanyId(1L)).thenReturn(List.of(project1));

		mockMvc.perform(get("/api/projects").param("companyId", "1"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json"))
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].name").value("Project Alpha"));
	}

	@Test
	void getProjects_withoutCompanyId_returnsAllProjects() throws Exception {
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

	@Test
	void getProjectSummaries_returnsOkWithJsonArray() throws Exception {
		when(clientService.listProjectSummaries())
				.thenReturn(List.of(new ProjectSummaryDto(1L, "Project Alpha", 1L, "Acme Corp", 2L)));

		mockMvc.perform(get("/api/projects/summaries"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json"))
				.andExpect(jsonPath("$[0].name").value("Project Alpha"))
				.andExpect(jsonPath("$[0].employeeCount").value(2));
	}

	@Test
	void getProject_existingId_returnsOkWithDetail() throws Exception {
		when(clientService.getProjectDetail(1L)).thenReturn(new ProjectDetailDto(
				1L, "Project Alpha", 1L, "Acme Corp", List.of(new AssignedEmployeeDto(5L, "Alice"))));

		mockMvc.perform(get("/api/projects/1"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json"))
				.andExpect(jsonPath("$.name").value("Project Alpha"))
				.andExpect(jsonPath("$.employees[0].name").value("Alice"));
	}

	@Test
	void createProject_validRequest_returns201WithCreatedDetail() throws Exception {
		when(clientService.createProject(new CreateProjectRequest("Project Gamma", 1L, null)))
				.thenReturn(new ProjectDetailDto(2L, "Project Gamma", 1L, "Acme Corp", List.of()));

		String requestBody = """
				{
					"name": "Project Gamma",
					"companyId": 1
				}
				""";

		mockMvc.perform(post("/api/projects")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isCreated())
				.andExpect(content().contentType("application/json"))
				.andExpect(jsonPath("$.id").value(2))
				.andExpect(jsonPath("$.name").value("Project Gamma"));
	}

	@Test
	void updateProject_validRequest_returnsOkWithUpdatedDetail() throws Exception {
		when(clientService.updateProject(1L, new UpdateProjectRequest("Project Alpha Renamed", null)))
				.thenReturn(new ProjectDetailDto(1L, "Project Alpha Renamed", 1L, "Acme Corp", List.of()));

		String requestBody = """
				{
					"name": "Project Alpha Renamed"
				}
				""";

		mockMvc.perform(put("/api/projects/1")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json"))
				.andExpect(jsonPath("$.name").value("Project Alpha Renamed"));
	}
}
