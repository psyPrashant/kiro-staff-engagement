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

	// --- Total Record Count Verification ---

	@Test
	void seedDataLoader_createsExactlyFourUsers() {
		assertThat(userRepository.count()).isEqualTo(4);
	}

	@Test
	void seedDataLoader_createsExactlyThirteenEmployees() {
		assertThat(employeeRepository.count()).isEqualTo(13);
	}

	@Test
	void seedDataLoader_createsExpectedInteractions() {
		// 20 interactions total (various employees, realistic scenario)
		assertThat(interactionRepository.count()).isEqualTo(20);
	}

	@Test
	void seedDataLoader_createsExpectedTasks() {
		// 17 tasks total
		assertThat(taskRepository.count()).isEqualTo(17);
	}

	@Test
	void seedDataLoader_createsTwentySixScheduledInteractions() {
		assertThat(scheduledInteractionRepository.count()).isEqualTo(26);
	}

	@Test
	void seedDataLoader_createsExactlyFourCompanies() {
		assertThat(companyRepository.count()).isEqualTo(4);
	}

	@Test
	void seedDataLoader_createsExactlySixProjects() {
		assertThat(projectRepository.count()).isEqualTo(6);
	}

	// --- Key Users Exist ---

	@Test
	void seedDataLoader_preservesPrimaryLoginUser() {
		assertThat(userRepository.findByEmail("alice.johnson@psybergate.com")).isPresent();
	}

	@Test
	void seedDataLoader_createsAllFourUsers() {
		assertThat(userRepository.findByEmail("alice.johnson@psybergate.com")).isPresent();
		assertThat(userRepository.findByEmail("marcus.vanderberg@psybergate.com")).isPresent();
		assertThat(userRepository.findByEmail("priya.naidoo@psybergate.com")).isPresent();
		assertThat(userRepository.findByEmail("thabo.molefe@psybergate.com")).isPresent();
	}

	// --- Basic Structural Verification ---

	@Test
	void seedDataLoader_shouldAssignManagerToSomeEmployees() {
		List<Employee> employees = employeeRepository.findAll();
		long managedCount = employees.stream()
				.filter(e -> e.getManager() != null)
				.count();
		// 11 employees have managers (all except Sipho and Fatima)
		assertThat(managedCount).isGreaterThanOrEqualTo(8);
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
