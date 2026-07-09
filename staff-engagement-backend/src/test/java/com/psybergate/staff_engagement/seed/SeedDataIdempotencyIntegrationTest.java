package com.psybergate.staff_engagement.seed;

import com.psybergate.staff_engagement.BaseIntegrationTest;
import com.psybergate.staff_engagement.client.CompanyRepository;
import com.psybergate.staff_engagement.client.ProjectRepository;
import com.psybergate.staff_engagement.employee.EmployeeRepository;
import com.psybergate.staff_engagement.interaction.InteractionRepository;
import com.psybergate.staff_engagement.task.TaskRepository;
import com.psybergate.staff_engagement.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Integration test verifying that the SeedDataLoader is idempotent:
 * running it multiple times does not create duplicate records.
 *
 * Validates: Requirements 1.4, 2.4
 */
class SeedDataIdempotencyIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private SeedDataLoader seedDataLoader;

	@Autowired
	private ApplicationArguments applicationArguments;

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
	void secondRun_doesNotCreateDuplicateRecords() {
		// The SeedDataLoader already ran on context startup (local profile active).
		// Capture row counts after the initial load.
		long usersAfterFirstRun = userRepository.count();
		long companiesAfterFirstRun = companyRepository.count();
		long employeesAfterFirstRun = employeeRepository.count();
		long projectsAfterFirstRun = projectRepository.count();
		long interactionsAfterFirstRun = interactionRepository.count();
		long tasksAfterFirstRun = taskRepository.count();

		// Sanity check: seed data is actually present
		assertThat(usersAfterFirstRun).isEqualTo(3);
		assertThat(companiesAfterFirstRun).isEqualTo(2);
		assertThat(employeesAfterFirstRun).isEqualTo(5);
		assertThat(projectsAfterFirstRun).isEqualTo(3);
		assertThat(interactionsAfterFirstRun).isEqualTo(4);
		assertThat(tasksAfterFirstRun).isEqualTo(3);

		// Act: trigger the loader a second time — should skip without error
		assertThatCode(() -> seedDataLoader.run(applicationArguments))
				.doesNotThrowAnyException();

		// Assert: row counts remain unchanged — no duplicates
		assertThat(userRepository.count()).isEqualTo(usersAfterFirstRun);
		assertThat(companyRepository.count()).isEqualTo(companiesAfterFirstRun);
		assertThat(employeeRepository.count()).isEqualTo(employeesAfterFirstRun);
		assertThat(projectRepository.count()).isEqualTo(projectsAfterFirstRun);
		assertThat(interactionRepository.count()).isEqualTo(interactionsAfterFirstRun);
		assertThat(taskRepository.count()).isEqualTo(tasksAfterFirstRun);
	}

	@Test
	void multipleRuns_noDuplicateEmailConstraintViolation() {
		// Running the loader multiple additional times should remain safe
		assertThatCode(() -> {
			seedDataLoader.run(applicationArguments);
			seedDataLoader.run(applicationArguments);
			seedDataLoader.run(applicationArguments);
		}).doesNotThrowAnyException();

		// Unique email constraints are still satisfied — counts unchanged
		assertThat(userRepository.count()).isEqualTo(3);
		assertThat(employeeRepository.count()).isEqualTo(5);
	}
}
