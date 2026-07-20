package com.psybergate.staff_engagement.client.service;

import com.psybergate.staff_engagement.client.domain.Company;
import com.psybergate.staff_engagement.client.domain.Project;
import com.psybergate.staff_engagement.client.dto.CreateCompanyRequest;
import com.psybergate.staff_engagement.client.dto.CreateProjectRequest;
import com.psybergate.staff_engagement.client.dto.ProjectDetailDto;
import com.psybergate.staff_engagement.client.dto.ProjectSummaryDto;
import com.psybergate.staff_engagement.client.dto.UpdateProjectRequest;
import java.util.List;

/**
 * Read/write operations for companies and projects backing the Companies page.
 *
 * <p>"Employees assigned" to a project is derived from interactions: an employee
 * is considered assigned to a project when they have at least one interaction
 * logged against it. There is no dedicated employee-project assignment table.
 */
public interface ClientService {

	/**
	 * Lists every company.
	 *
	 * @return all companies
	 */
	List<Company> listCompanies();

	/**
	 * Lists projects, optionally narrowed to a single company.
	 *
	 * @param companyId keep only projects for this company; {@code null} returns all projects
	 * @return the matching projects
	 */
	List<Project> listProjects(Long companyId);

	/**
	 * Lists every project with its owning company and a count of assigned employees.
	 *
	 * @return one summary per project
	 */
	List<ProjectSummaryDto> listProjectSummaries();

	/**
	 * Loads a single project together with the employees assigned to it.
	 *
	 * @param id the project to load
	 * @return the project detail
	 * @throws org.springframework.web.server.ResponseStatusException {@code 404} if the project does not exist
	 */
	ProjectDetailDto getProjectDetail(Long id);

	/**
	 * Creates a project under either an existing company ({@code companyId}) or a
	 * newly created one ({@code newCompanyName}).
	 *
	 * @param request the project to create
	 * @return the created project detail
	 * @throws org.springframework.web.server.ResponseStatusException {@code 400} if the name is blank,
	 *                                                               or neither company reference resolves
	 */
	ProjectDetailDto createProject(CreateProjectRequest request);

	/**
	 * Renames a project and optionally moves it to a different company.
	 *
	 * @param id      the project to update
	 * @param request the new name and optional company
	 * @return the updated project detail
	 * @throws org.springframework.web.server.ResponseStatusException {@code 404} if the project does not exist,
	 *                                                               {@code 400} if the name is blank or the
	 *                                                               company does not exist
	 */
	ProjectDetailDto updateProject(Long id, UpdateProjectRequest request);

	/**
	 * Creates a company.
	 *
	 * @param request the company to create
	 * @return the persisted company
	 * @throws org.springframework.web.server.ResponseStatusException {@code 400} if the name is blank
	 */
	Company createCompany(CreateCompanyRequest request);
}
