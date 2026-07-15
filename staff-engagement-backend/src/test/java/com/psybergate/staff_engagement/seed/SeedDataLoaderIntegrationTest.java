package com.psybergate.staff_engagement.seed;

import com.psybergate.staff_engagement.BaseIntegrationTest;
import com.psybergate.staff_engagement.client.CompanyRepository;
import com.psybergate.staff_engagement.client.ProjectRepository;
import com.psybergate.staff_engagement.employee.Employee;
import com.psybergate.staff_engagement.employee.EmployeeRepository;
import com.psybergate.staff_engagement.interaction.Interaction;
import com.psybergate.staff_engagement.interaction.InteractionRepository;
import com.psybergate.staff_engagement.scheduling.ScheduledInteractionRepository;
import com.psybergate.staff_engagement.task.Task;
import com.psybergate.staff_engagement.task.TaskRepository;
import com.psybergate.staff_engagement.task.TaskStatus;
import com.psybergate.staff_engagement.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Transactional
class SeedDataLoaderIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CompanyRepository companyRepository;

	@Autowired
	private EmployeeRepository employeeRepository;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private InteractionRepository interactionRepository;

	@Autowired
	private TaskRepository taskRepository;

	@Autowired
	private ScheduledInteractionRepository scheduledInteractionRepository;

	// --- Total Record Count Verification (Requirements 6.1, 6.2, 1.1, 2.1, 3.1, 4.1, 5.1) ---

	@Test
	void seedDataLoader_createsExactlyFiveUsers() {
		assertThat(userRepository.count()).isEqualTo(5);
	}

	@Test
	void seedDataLoader_createsExactlyTwentyFiveEmployees() {
		assertThat(employeeRepository.count()).isEqualTo(25);
	}

	@Test
	void seedDataLoader_createsExactlyFourHundredFourInteractions() {
		assertThat(interactionRepository.count()).isEqualTo(404);
	}

	@Test
	void seedDataLoader_createsExactlyTwentyEightTasks() {
		assertThat(taskRepository.count()).isEqualTo(28);
	}

	@Test
	void seedDataLoader_createsExactlyFifteenScheduledInteractions() {
		assertThat(scheduledInteractionRepository.count()).isEqualTo(15);
	}

	@Test
	void seedDataLoader_createsExactlyTwoCompanies() {
		assertThat(companyRepository.count()).isEqualTo(2);
	}

	@Test
	void seedDataLoader_createsExactlyThreeProjects() {
		assertThat(projectRepository.count()).isEqualTo(3);
	}

	// --- Existing Data Preservation (Requirement 6.1) ---

	@Test
	void seedDataLoader_preservesOriginalThreeUsers() {
		assertThat(userRepository.findByEmail("alice.johnson@psybergate.com")).isPresent();
		assertThat(userRepository.findByEmail("bob.smith@psybergate.com")).isPresent();
		assertThat(userRepository.findByEmail("carol.williams@psybergate.com")).isPresent();
	}

	@Test
	void seedDataLoader_preservesOriginalFiveEmployees() {
		List<Employee> employees = employeeRepository.findAll();
		// Original 5 employees do not have @company.com emails
		long originalEmployeeCount = employees.stream()
				.filter(e -> !e.getEmail().endsWith("@company.com"))
				.count();
		assertThat(originalEmployeeCount).isEqualTo(5);
	}

	@Test
	void seedDataLoader_preservesOriginalFourInteractions() {
		List<Interaction> interactions = interactionRepository.findAll();
		// Original 4 interactions belong to original employees (not @company.com)
		long originalInteractionCount = interactions.stream()
				.filter(i -> !i.getEmployee().getEmail().endsWith("@company.com"))
				.count();
		assertThat(originalInteractionCount).isEqualTo(4);
	}

	@Test
	void seedDataLoader_preservesOriginalThreeTasks() {
		List<Task> tasks = taskRepository.findAll();
		// Original 3 tasks are linked to interactions of original employees
		long originalTaskCount = tasks.stream()
				.filter(t -> t.getInteraction() != null
						&& !t.getInteraction().getEmployee().getEmail().endsWith("@company.com"))
				.count();
		assertThat(originalTaskCount).isEqualTo(3);
	}

	// --- Basic Structural Verification ---

	@Test
	void seedDataLoader_shouldAssignManagerToAtLeastTenNewEmployees() {
		List<Employee> employees = employeeRepository.findAll();
		long managedCount = employees.stream()
				.filter(e -> e.getEmail().endsWith("@company.com"))
				.filter(e -> e.getManager() != null)
				.count();
		assertThat(managedCount).isGreaterThanOrEqualTo(10);
	}

	@Test
	void seedDataLoader_shouldCoverAllFourInteractionTypes() {
		List<Interaction> interactions = interactionRepository.findAll();
		long distinctTypeCount = interactions.stream()
				.map(Interaction::getType)
				.distinct()
				.count();
		assertThat(distinctTypeCount).isEqualTo(4);
	}

	@Test
	void seedDataLoader_shouldIncludeOpenAndDoneTasks() {
		List<Task> tasks = taskRepository.findAll();
		assertThat(tasks).anyMatch(t -> t.getStatus() == TaskStatus.OPEN);
		assertThat(tasks).anyMatch(t -> t.getStatus() == TaskStatus.DONE);
	}
}
