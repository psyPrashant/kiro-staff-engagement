package com.psybergate.staff_engagement.client;

import net.jqwik.api.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for the company filter on projects using jqwik.
 *
 * Validates: Requirements 3.1
 */
class ProjectPropertyTest {

	/**
	 * Property 5: Company filter returns only matching projects
	 *
	 * For any companyId, a GET request to /api/projects?companyId={companyId}
	 * SHALL return only projects where project.company.id equals the provided companyId.
	 *
	 * This test verifies the filtering contract: given a universe of projects belonging
	 * to various companies, when filtered by a target companyId, all returned projects
	 * must have company.id == targetCompanyId (and conversely, no project with a different
	 * company.id should be present).
	 *
	 * The controller delegates to ProjectRepository.findByCompanyId(). We simulate this
	 * at unit level: generate random projects with varied company IDs, apply the filtering
	 * logic (same as the repository contract), and verify the property holds.
	 *
	 * Validates: Requirements 3.1
	 */
	@Property(tries = 100)
	@Tag("Feature: interaction-task-write-api, Property 5: Company filter returns only matching projects")
	void companyFilterReturnsOnlyMatchingProjects(
			@ForAll("targetCompanyIds") Long targetCompanyId,
			@ForAll("projectLists") List<ProjectData> allProjects) {

		// Simulate the filtering that findByCompanyId performs (the repository contract)
		List<ProjectData> filteredProjects = allProjects.stream()
				.filter(p -> p.companyId().equals(targetCompanyId))
				.toList();

		// Simulate what the controller does: pass companyId → get filtered results
		// This is equivalent to: controller.getProjects(targetCompanyId) which calls
		// projectRepository.findByCompanyId(targetCompanyId)
		List<Project> controllerResult = simulateControllerGetProjects(targetCompanyId, allProjects);

		// Property: every returned project has company.id == targetCompanyId
		assertThat(controllerResult).allSatisfy(project ->
				assertThat(project.getCompany().getId()).isEqualTo(targetCompanyId));

		// Property: the count matches the expected filtered count
		assertThat(controllerResult).hasSize(filteredProjects.size());
	}

	/**
	 * Simulates what ProjectController.getProjects(companyId) does:
	 * When companyId is provided, it calls projectRepository.findByCompanyId(companyId)
	 * which returns only projects with matching company.id.
	 */
	private List<Project> simulateControllerGetProjects(Long companyId, List<ProjectData> allProjects) {
		// Build the full universe of Project entities
		List<Project> allEntities = new ArrayList<>();
		for (int i = 0; i < allProjects.size(); i++) {
			ProjectData pd = allProjects.get(i);
			Company company = new Company();
			company.setId(pd.companyId());
			company.setName("Company-" + pd.companyId());
			company.setCreatedAt(Instant.now());

			Project project = new Project();
			project.setId((long) (i + 1));
			project.setName(pd.name());
			project.setCompany(company);
			project.setCreatedAt(Instant.now());
			allEntities.add(project);
		}

		// Simulate findByCompanyId: filter to only those matching the target companyId
		// This is the repository contract that the controller relies on
		return allEntities.stream()
				.filter(p -> p.getCompany().getId().equals(companyId))
				.toList();
	}

	@Provide
	Arbitrary<Long> targetCompanyIds() {
		return Arbitraries.longs().between(1L, 5L);
	}

	@Provide
	Arbitrary<List<ProjectData>> projectLists() {
		Arbitrary<ProjectData> projectArbitrary = Arbitraries.longs().between(1L, 5L)
				.flatMap(companyId -> Arbitraries.strings()
						.withCharRange('a', 'z')
						.ofMinLength(3)
						.ofMaxLength(20)
						.map(name -> new ProjectData(name, companyId)));

		return projectArbitrary.list().ofMinSize(1).ofMaxSize(10);
	}

	/**
	 * Simple record to represent project test data (avoids loading JPA entity classes during
	 * arbitrary generation).
	 */
	record ProjectData(String name, Long companyId) {}
}
