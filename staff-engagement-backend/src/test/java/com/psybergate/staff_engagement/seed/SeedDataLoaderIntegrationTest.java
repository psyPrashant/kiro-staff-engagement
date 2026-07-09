package com.psybergate.staff_engagement.seed;

import com.psybergate.staff_engagement.BaseIntegrationTest;
import com.psybergate.staff_engagement.client.CompanyRepository;
import com.psybergate.staff_engagement.client.ProjectRepository;
import com.psybergate.staff_engagement.employee.Employee;
import com.psybergate.staff_engagement.employee.EmployeeRepository;
import com.psybergate.staff_engagement.interaction.Interaction;
import com.psybergate.staff_engagement.interaction.InteractionRepository;
import com.psybergate.staff_engagement.task.Task;
import com.psybergate.staff_engagement.task.TaskRepository;
import com.psybergate.staff_engagement.task.TaskStatus;
import com.psybergate.staff_engagement.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

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

	@Test
	void seedDataLoader_shouldInsertMinimumUserRecords() {
		assertThat(userRepository.count()).isGreaterThanOrEqualTo(3);
	}

	@Test
	void seedDataLoader_shouldInsertMinimumCompanyRecords() {
		assertThat(companyRepository.count()).isGreaterThanOrEqualTo(2);
	}

	@Test
	void seedDataLoader_shouldInsertMinimumEmployeeRecords() {
		assertThat(employeeRepository.count()).isGreaterThanOrEqualTo(5);
	}

	@Test
	void seedDataLoader_shouldInsertMinimumProjectRecords() {
		assertThat(projectRepository.count()).isGreaterThanOrEqualTo(3);
	}

	@Test
	void seedDataLoader_shouldInsertMinimumInteractionRecords() {
		assertThat(interactionRepository.count()).isGreaterThanOrEqualTo(4);
	}

	@Test
	void seedDataLoader_shouldInsertMinimumTaskRecords() {
		assertThat(taskRepository.count()).isGreaterThanOrEqualTo(3);
	}

	@Test
	void seedDataLoader_shouldAssignManagerToAtLeastOneEmployee() {
		List<Employee> employees = employeeRepository.findAll();
		assertThat(employees)
				.anyMatch(e -> e.getManager() != null);
	}

	@Test
	void seedDataLoader_shouldCoverAtLeastTwoDistinctInteractionTypes() {
		List<Interaction> interactions = interactionRepository.findAll();
		long distinctTypeCount = interactions.stream()
				.map(Interaction::getType)
				.distinct()
				.count();
		assertThat(distinctTypeCount).isGreaterThanOrEqualTo(2);
	}

	@Test
	void seedDataLoader_shouldIncludeAtLeastOneOpenTask() {
		List<Task> tasks = taskRepository.findAll();
		assertThat(tasks)
				.anyMatch(t -> t.getStatus() == TaskStatus.OPEN);
	}

	@Test
	void seedDataLoader_shouldIncludeAtLeastOneDoneTask() {
		List<Task> tasks = taskRepository.findAll();
		assertThat(tasks)
				.anyMatch(t -> t.getStatus() == TaskStatus.DONE);
	}
}
