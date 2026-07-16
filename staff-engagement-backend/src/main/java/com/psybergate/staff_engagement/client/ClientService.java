package com.psybergate.staff_engagement.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.psybergate.staff_engagement.interaction.InteractionRepository;

import lombok.RequiredArgsConstructor;

/**
 * Read/write operations for companies and projects backing the Companies page.
 *
 * <p>"Employees assigned" to a project is derived from interactions: an employee
 * is considered assigned to a project when they have at least one interaction
 * logged against it. There is no dedicated employee-project assignment table.
 */
@Service
@RequiredArgsConstructor
public class ClientService {

	private final ProjectRepository projectRepository;
	private final CompanyRepository companyRepository;
	private final InteractionRepository interactionRepository;

	public List<ProjectSummaryDto> listProjectSummaries() {
		Map<Long, Long> employeeCounts = new HashMap<>();
		for (Object[] row : interactionRepository.countDistinctEmployeesGroupedByProject()) {
			employeeCounts.put((Long) row[0], (Long) row[1]);
		}

		return projectRepository.findAll().stream()
				.map(project -> new ProjectSummaryDto(
						project.getId(),
						project.getName(),
						project.getCompany().getId(),
						project.getCompany().getName(),
						employeeCounts.getOrDefault(project.getId(), 0L)))
				.toList();
	}

	public ProjectDetailDto getProjectDetail(Long id) {
		Project project = projectRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
		return toDetail(project);
	}

	@Transactional
	public ProjectDetailDto createProject(CreateProjectRequest request) {
		String name = trimmedOrThrow(request.name(), "Project name is required");
		Company company = resolveCompany(request.companyId(), request.newCompanyName());

		Project project = new Project();
		project.setName(name);
		project.setCompany(company);
		projectRepository.save(project);

		return toDetail(project);
	}

	@Transactional
	public ProjectDetailDto updateProject(Long id, UpdateProjectRequest request) {
		Project project = projectRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
		project.setName(trimmedOrThrow(request.name(), "Project name is required"));

		if (request.companyId() != null) {
			Company company = companyRepository.findById(request.companyId())
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Company not found"));
			project.setCompany(company);
		}
		projectRepository.save(project);

		return toDetail(project);
	}

	@Transactional
	public Company createCompany(CreateCompanyRequest request) {
		Company company = new Company();
		company.setName(trimmedOrThrow(request.name(), "Company name is required"));
		return companyRepository.save(company);
	}

	private ProjectDetailDto toDetail(Project project) {
		List<AssignedEmployeeDto> employees = interactionRepository
				.findDistinctEmployeesByProjectId(project.getId()).stream()
				.map(employee -> new AssignedEmployeeDto(employee.getId(), employee.getName()))
				.toList();

		return new ProjectDetailDto(
				project.getId(),
				project.getName(),
				project.getCompany().getId(),
				project.getCompany().getName(),
				employees);
	}

	private Company resolveCompany(Long companyId, String newCompanyName) {
		if (companyId != null) {
			return companyRepository.findById(companyId)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Company not found"));
		}
		String name = trimmedOrThrow(newCompanyName, "Either companyId or newCompanyName is required");
		Company company = new Company();
		company.setName(name);
		return companyRepository.save(company);
	}

	private String trimmedOrThrow(String value, String message) {
		String trimmed = value == null ? "" : value.trim();
		if (trimmed.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
		}
		return trimmed;
	}
}
