package com.psybergate.staff_engagement.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.psybergate.staff_engagement.client.domain.Company;
import com.psybergate.staff_engagement.client.domain.CompanyRepository;
import com.psybergate.staff_engagement.client.domain.Project;
import com.psybergate.staff_engagement.client.domain.ProjectRepository;
import com.psybergate.staff_engagement.client.dto.AssignedEmployeeDto;
import com.psybergate.staff_engagement.client.dto.CreateCompanyRequest;
import com.psybergate.staff_engagement.client.dto.CreateProjectRequest;
import com.psybergate.staff_engagement.client.dto.ProjectDetailDto;
import com.psybergate.staff_engagement.client.dto.ProjectSummaryDto;
import com.psybergate.staff_engagement.client.dto.UpdateProjectRequest;
import com.psybergate.staff_engagement.employee.domain.Employee;
import com.psybergate.staff_engagement.interaction.domain.InteractionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

	@Mock
	private ProjectRepository projectRepository;
	@Mock
	private CompanyRepository companyRepository;
	@Mock
	private InteractionRepository interactionRepository;

	private ClientService service() {
		return new ClientServiceImpl(projectRepository, companyRepository, interactionRepository);
	}

	private Company company(long id, String name) {
		Company company = new Company();
		company.setId(id);
		company.setName(name);
		return company;
	}

	private Project project(long id, String name, Company company) {
		Project project = new Project();
		project.setId(id);
		project.setName(name);
		project.setCompany(company);
		return project;
	}

	private Employee employee(long id, String name) {
		Employee employee = new Employee();
		employee.setId(id);
		employee.setName(name);
		return employee;
	}

	@Test
	void listProjectSummaries_countsDistinctEmployeesPerProject() {
		Company acme = company(1L, "Acme Corp");
		Project alpha = project(10L, "Alpha", acme);
		Project beta = project(11L, "Beta", acme);

		when(projectRepository.findAll()).thenReturn(List.of(alpha, beta));
		List<Object[]> counts = List.<Object[]>of(new Object[] {10L, 3L});
		when(interactionRepository.countDistinctEmployeesGroupedByProject()).thenReturn(counts);

		List<ProjectSummaryDto> summaries = service().listProjectSummaries();

		assertThat(summaries).containsExactly(
				new ProjectSummaryDto(10L, "Alpha", 1L, "Acme Corp", 3L),
				new ProjectSummaryDto(11L, "Beta", 1L, "Acme Corp", 0L));
	}

	@Test
	void getProjectDetail_existingProject_returnsDetailWithAssignedEmployees() {
		Company acme = company(1L, "Acme Corp");
		Project alpha = project(10L, "Alpha", acme);

		when(projectRepository.findById(10L)).thenReturn(Optional.of(alpha));
		when(interactionRepository.findDistinctEmployeesByProjectId(10L))
				.thenReturn(List.of(employee(5L, "Alice")));

		ProjectDetailDto detail = service().getProjectDetail(10L);

		assertThat(detail).isEqualTo(new ProjectDetailDto(
				10L, "Alpha", 1L, "Acme Corp", List.of(new AssignedEmployeeDto(5L, "Alice"))));
	}

	@Test
	void getProjectDetail_missingProject_throwsNotFound() {
		when(projectRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service().getProjectDetail(99L))
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(404));
	}

	@Test
	void createProject_withExistingCompanyId_savesAndReturnsDetail() {
		Company acme = company(1L, "Acme Corp");
		when(companyRepository.findById(1L)).thenReturn(Optional.of(acme));
		when(interactionRepository.findDistinctEmployeesByProjectId(any())).thenReturn(List.of());

		ProjectDetailDto detail = service().createProject(new CreateProjectRequest("Gamma", 1L, null));

		assertThat(detail.name()).isEqualTo("Gamma");
		assertThat(detail.companyId()).isEqualTo(1L);
		verify(projectRepository).save(any(Project.class));
		verify(companyRepository, never()).save(any());
	}

	@Test
	void createProject_withNewCompanyName_createsCompanyThenProject() {
		when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> {
			Company saved = invocation.getArgument(0);
			saved.setId(2L);
			return saved;
		});
		when(interactionRepository.findDistinctEmployeesByProjectId(any())).thenReturn(List.of());

		ProjectDetailDto detail = service().createProject(new CreateProjectRequest("Delta", null, "Globex Inc"));

		assertThat(detail.companyId()).isEqualTo(2L);
		assertThat(detail.companyName()).isEqualTo("Globex Inc");
		verify(projectRepository).save(any(Project.class));
	}

	@Test
	void createProject_blankName_throwsBadRequest() {
		assertThatThrownBy(() -> service().createProject(new CreateProjectRequest("  ", 1L, null)))
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
	}

	@Test
	void createProject_neitherCompanyIdNorNewCompanyName_throwsBadRequest() {
		assertThatThrownBy(() -> service().createProject(new CreateProjectRequest("Gamma", null, null)))
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
	}

	@Test
	void createProject_unknownCompanyId_throwsBadRequest() {
		when(companyRepository.findById(404L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service().createProject(new CreateProjectRequest("Gamma", 404L, null)))
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
	}

	@Test
	void updateProject_rename_savesUpdatedName() {
		Company acme = company(1L, "Acme Corp");
		Project alpha = project(10L, "Alpha", acme);
		when(projectRepository.findById(10L)).thenReturn(Optional.of(alpha));
		when(interactionRepository.findDistinctEmployeesByProjectId(10L)).thenReturn(List.of());

		ProjectDetailDto detail = service().updateProject(10L, new UpdateProjectRequest("Alpha Renamed", null));

		assertThat(detail.name()).isEqualTo("Alpha Renamed");
		assertThat(detail.companyId()).isEqualTo(1L);
		verify(projectRepository).save(alpha);
	}

	@Test
	void updateProject_withNewCompanyId_movesProjectToCompany() {
		Company acme = company(1L, "Acme Corp");
		Company globex = company(2L, "Globex Inc");
		Project alpha = project(10L, "Alpha", acme);
		when(projectRepository.findById(10L)).thenReturn(Optional.of(alpha));
		when(companyRepository.findById(2L)).thenReturn(Optional.of(globex));
		when(interactionRepository.findDistinctEmployeesByProjectId(10L)).thenReturn(List.of());

		ProjectDetailDto detail = service().updateProject(10L, new UpdateProjectRequest("Alpha", 2L));

		assertThat(detail.companyId()).isEqualTo(2L);
		assertThat(detail.companyName()).isEqualTo("Globex Inc");
	}

	@Test
	void updateProject_missingProject_throwsNotFound() {
		when(projectRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service().updateProject(99L, new UpdateProjectRequest("Alpha", null)))
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(404));
	}

	@Test
	void updateProject_unknownCompanyId_throwsBadRequest() {
		Company acme = company(1L, "Acme Corp");
		Project alpha = project(10L, "Alpha", acme);
		when(projectRepository.findById(10L)).thenReturn(Optional.of(alpha));
		when(companyRepository.findById(404L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service().updateProject(10L, new UpdateProjectRequest("Alpha", 404L)))
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
	}

	@Test
	void createCompany_validName_savesAndReturnsCompany() {
		when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Company created = service().createCompany(new CreateCompanyRequest("Initech"));

		assertThat(created.getName()).isEqualTo("Initech");
		verify(projectRepository, never()).save(any());
	}

	@Test
	void createCompany_blankName_throwsBadRequest() {
		assertThatThrownBy(() -> service().createCompany(new CreateCompanyRequest(" ")))
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
	}

	// --- listCompanies ------------------------------------------------------

	@Test
	void listCompanies_returnsEveryCompany() {
		Company acme = company(1L, "Acme Corp");
		Company initech = company(2L, "Initech");
		when(companyRepository.findAll()).thenReturn(List.of(acme, initech));

		assertThat(service().listCompanies()).containsExactly(acme, initech);
	}

	// --- listProjects -------------------------------------------------------

	@Test
	void listProjects_withCompanyId_narrowsToThatCompany() {
		Project alpha = project(1L, "Project Alpha", company(1L, "Acme Corp"));
		when(projectRepository.findByCompanyId(1L)).thenReturn(List.of(alpha));

		assertThat(service().listProjects(1L)).containsExactly(alpha);
		verify(projectRepository, never()).findAll();
	}

	@Test
	void listProjects_withNullCompanyId_returnsEveryProject() {
		Company acme = company(1L, "Acme Corp");
		Project alpha = project(1L, "Project Alpha", acme);
		Project beta = project(2L, "Project Beta", acme);
		when(projectRepository.findAll()).thenReturn(List.of(alpha, beta));

		assertThat(service().listProjects(null)).containsExactly(alpha, beta);
		verify(projectRepository, never()).findByCompanyId(any());
	}
}
