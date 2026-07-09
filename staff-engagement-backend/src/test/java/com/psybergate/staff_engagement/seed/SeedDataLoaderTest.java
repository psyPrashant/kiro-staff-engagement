package com.psybergate.staff_engagement.seed;

import com.psybergate.staff_engagement.client.Company;
import com.psybergate.staff_engagement.client.CompanyRepository;
import com.psybergate.staff_engagement.client.Project;
import com.psybergate.staff_engagement.client.ProjectRepository;
import com.psybergate.staff_engagement.employee.Employee;
import com.psybergate.staff_engagement.employee.EmployeeRepository;
import com.psybergate.staff_engagement.interaction.Interaction;
import com.psybergate.staff_engagement.interaction.InteractionRepository;
import com.psybergate.staff_engagement.task.Task;
import com.psybergate.staff_engagement.task.TaskRepository;
import com.psybergate.staff_engagement.user.User;
import com.psybergate.staff_engagement.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeedDataLoaderTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private CompanyRepository companyRepository;

	@Mock
	private EmployeeRepository employeeRepository;

	@Mock
	private ProjectRepository projectRepository;

	@Mock
	private InteractionRepository interactionRepository;

	@Mock
	private TaskRepository taskRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private ApplicationArguments applicationArguments;

	@InjectMocks
	private SeedDataLoader seedDataLoader;

	@Test
	void run_whenSeedDataAlreadyPresent_skipsInsertionAndCallsNoSave() {
		// Arrange: findByEmail returns an existing user → seed data already present
		when(userRepository.findByEmail("alice.johnson@psybergate.com"))
				.thenReturn(Optional.of(new User()));

		// Act
		seedDataLoader.run(applicationArguments);

		// Assert: no save() calls on any repository
		verify(userRepository, never()).save(any(User.class));
		verify(companyRepository, never()).save(any(Company.class));
		verify(employeeRepository, never()).save(any(Employee.class));
		verify(projectRepository, never()).save(any(Project.class));
		verify(interactionRepository, never()).save(any(Interaction.class));
		verify(taskRepository, never()).save(any(Task.class));
	}

	@Test
	void run_whenSeedDataNotPresent_insertsInCorrectForeignKeyOrder() {
		// Arrange: findByEmail returns empty → seed data not present
		when(userRepository.findByEmail("alice.johnson@psybergate.com"))
				.thenReturn(Optional.empty());

		// Stub passwordEncoder to return a fake hash
		when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$fakehashvalue");

		// Stub save() to return the argument back (entities need to be returned for FK references)
		when(userRepository.save(any(User.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(companyRepository.save(any(Company.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(employeeRepository.save(any(Employee.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(projectRepository.save(any(Project.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(interactionRepository.save(any(Interaction.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(taskRepository.save(any(Task.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		// Act
		seedDataLoader.run(applicationArguments);

		// Assert: verify insertion order respects FK dependencies
		// Users and Companies must be saved before Employees
		// Employees must be saved before Projects (Projects depend on Companies, not Employees,
		// but Interactions depend on both Employees and Users)
		// Interactions must be saved before Tasks
		InOrder inOrder = inOrder(userRepository, companyRepository, employeeRepository,
				projectRepository, interactionRepository, taskRepository);

		// 1. Users (3 saves)
		inOrder.verify(userRepository, times(3)).save(any(User.class));
		// 2. Companies (2 saves)
		inOrder.verify(companyRepository, times(2)).save(any(Company.class));
		// 3. Employees (5 saves — 3 without manager, then 2 with manager)
		inOrder.verify(employeeRepository, times(5)).save(any(Employee.class));
		// 4. Projects (3 saves, depend on Companies)
		inOrder.verify(projectRepository, times(3)).save(any(Project.class));
		// 5. Interactions (4 saves, depend on Employees + Users + optionally Projects)
		inOrder.verify(interactionRepository, times(4)).save(any(Interaction.class));
		// 6. Tasks (3 saves, depend on Interactions + optionally Users)
		inOrder.verify(taskRepository, times(3)).save(any(Task.class));
	}

	@Test
	void run_idempotency_noSaveCallsWhenSeedDataAlreadyPresent() {
		// Arrange: simulate second execution where seed data exists
		when(userRepository.findByEmail("alice.johnson@psybergate.com"))
				.thenReturn(Optional.of(new User()));

		// Act: run twice to simulate re-execution
		seedDataLoader.run(applicationArguments);
		seedDataLoader.run(applicationArguments);

		// Assert: findByEmail called twice (once per run), but never any save
		verify(userRepository, times(2)).findByEmail("alice.johnson@psybergate.com");
		verify(userRepository, never()).save(any(User.class));
		verify(companyRepository, never()).save(any(Company.class));
		verify(employeeRepository, never()).save(any(Employee.class));
		verify(projectRepository, never()).save(any(Project.class));
		verify(interactionRepository, never()).save(any(Interaction.class));
		verify(taskRepository, never()).save(any(Task.class));
	}
}
