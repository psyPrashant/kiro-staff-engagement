package com.psybergate.staff_engagement.employee360;

import com.psybergate.staff_engagement.client.Company;
import com.psybergate.staff_engagement.client.Project;
import com.psybergate.staff_engagement.employee.Employee;
import com.psybergate.staff_engagement.employee.EmployeeRepository;
import com.psybergate.staff_engagement.interaction.Interaction;
import com.psybergate.staff_engagement.interaction.InteractionRepository;
import com.psybergate.staff_engagement.interaction.InteractionType;
import com.psybergate.staff_engagement.task.TaskRepository;
import com.psybergate.staff_engagement.task.TaskStatus;
import com.psybergate.staff_engagement.user.User;
import net.jqwik.api.*;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Property-based test for Employee 360 project context biconditional.
 *
 * Validates: Requirements 1.4, 2.3
 */
@Tag("Feature: employee-360-view, Property 4: Project context is present iff interaction has a project")
class Employee360ProjectContextPropertyTest {

	private final EmployeeRepository employeeRepository = Mockito.mock(EmployeeRepository.class);
	private final InteractionRepository interactionRepository = Mockito.mock(InteractionRepository.class);
	private final TaskRepository taskRepository = Mockito.mock(TaskRepository.class);
	private final Employee360Service service = new Employee360Service(employeeRepository, interactionRepository, taskRepository);

	/**
	 * Property 4: Project context is present if and only if the interaction has a project
	 *
	 * For any interaction in the response, projectContext SHALL be non-null if and only if
	 * the source Interaction entity has a non-null Project reference. When present,
	 * projectName and companyName SHALL match the source Project and Company entity names.
	 *
	 * Validates: Requirements 1.4, 2.3
	 */
	@Property(tries = 100)
	void projectContextPresentIffInteractionHasProject(
			@ForAll("interactionsWithMixedProjects") List<Interaction> interactions) {

		// Arrange: set up employee
		Employee employee = createEmployee(1L, "Test Employee");
		when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
		when(interactionRepository.findByEmployeeIdOrderByOccurredAtDesc(1L)).thenReturn(interactions);

		List<Long> interactionIds = interactions.stream().map(Interaction::getId).toList();
		if (!interactionIds.isEmpty()) {
			when(taskRepository.findByInteractionIdInAndStatus(eq(interactionIds), eq(TaskStatus.OPEN)))
					.thenReturn(List.of());
		}

		// Act
		Employee360Response response = service.getEmployee360(1L);

		// Assert: biconditional for each interaction
		assertThat(response.interactions()).hasSameSizeAs(interactions);

		for (int i = 0; i < interactions.size(); i++) {
			Interaction source = interactions.get(i);
			InteractionDto dto = response.interactions().get(i);

			if (source.getProject() == null) {
				assertThat(dto.projectContext())
						.as("Interaction %d has no project, projectContext should be null", source.getId())
						.isNull();
			} else {
				assertThat(dto.projectContext())
						.as("Interaction %d has a project, projectContext should be non-null", source.getId())
						.isNotNull();
				assertThat(dto.projectContext().projectName())
						.as("projectName should match source project name")
						.isEqualTo(source.getProject().getName());
				assertThat(dto.projectContext().companyName())
						.as("companyName should match source project's company name")
						.isEqualTo(source.getProject().getCompany().getName());
			}
		}
	}

	@Provide
	Arbitrary<List<Interaction>> interactionsWithMixedProjects() {
		return interactionArbitrary().list().ofMinSize(1).ofMaxSize(10);
	}

	private Arbitrary<Interaction> interactionArbitrary() {
		Arbitrary<Boolean> hasProject = Arbitraries.of(true, false);
		Arbitrary<String> projectNames = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50);
		Arbitrary<String> companyNames = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50);
		Arbitrary<InteractionType> types = Arbitraries.of(InteractionType.values());
		Arbitrary<Long> ids = Arbitraries.longs().between(1L, 10000L);
		Arbitrary<String> notes = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(100);
		Arbitrary<String> userNames = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(30);

		return Combinators.combine(hasProject, projectNames, companyNames, types, ids, notes, userNames)
				.as((withProject, projName, compName, type, id, note, userName) -> {
					Interaction interaction = new Interaction();
					interaction.setId(id);
					interaction.setEmployee(createEmployee(1L, "Test Employee"));
					interaction.setConductedBy(createUser(userName));
					interaction.setLoggedBy(createUser("Logger"));
					interaction.setType(type);
					interaction.setNotes(note);
					interaction.setOccurredAt(Instant.now());
					interaction.setCreatedAt(Instant.now());

					if (withProject) {
						Company company = new Company();
						company.setId(1L);
						company.setName(compName);
						company.setCreatedAt(Instant.now());

						Project project = new Project();
						project.setId(1L);
						project.setName(projName);
						project.setCompany(company);
						project.setCreatedAt(Instant.now());

						interaction.setProject(project);
					} else {
						interaction.setProject(null);
					}

					return interaction;
				});
	}

	private Employee createEmployee(Long id, String name) {
		Employee employee = new Employee();
		employee.setId(id);
		employee.setName(name);
		employee.setEmail(name.toLowerCase().replace(" ", ".") + "@test.com");
		employee.setJobTitle("Engineer");
		employee.setCreatedAt(Instant.now());
		return employee;
	}

	private User createUser(String name) {
		User user = new User();
		user.setId(1L);
		user.setName(name);
		user.setEmail(name.toLowerCase().replace(" ", ".") + "@test.com");
		user.setCreatedAt(Instant.now());
		return user;
	}
}
