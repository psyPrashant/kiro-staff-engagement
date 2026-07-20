package com.psybergate.staff_engagement.seed;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.psybergate.staff_engagement.client.domain.Company;
import com.psybergate.staff_engagement.client.domain.CompanyRepository;
import com.psybergate.staff_engagement.client.domain.Project;
import com.psybergate.staff_engagement.client.domain.ProjectRepository;
import com.psybergate.staff_engagement.employee.domain.Employee;
import com.psybergate.staff_engagement.employee.domain.EmployeeRepository;
import com.psybergate.staff_engagement.interaction.domain.Interaction;
import com.psybergate.staff_engagement.interaction.domain.InteractionRepository;
import com.psybergate.staff_engagement.scheduling.domain.ScheduledInteraction;
import com.psybergate.staff_engagement.scheduling.domain.ScheduledInteractionRepository;
import com.psybergate.staff_engagement.task.domain.Task;
import com.psybergate.staff_engagement.task.domain.TaskRepository;
import com.psybergate.staff_engagement.user.domain.User;
import com.psybergate.staff_engagement.user.domain.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

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
	private ScheduledInteractionRepository scheduledInteractionRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private ApplicationArguments applicationArguments;

	@InjectMocks
	private SeedDataLoader seedDataLoader;

	@Test
	void run_whenSeedDataAlreadyPresent_skipsInsertionAndCallsNoSave() {
		when(userRepository.findByEmail("alice.johnson@psybergate.com"))
				.thenReturn(Optional.of(new User()));

		seedDataLoader.run(applicationArguments);

		verify(userRepository, never()).save(any(User.class));
		verify(companyRepository, never()).save(any(Company.class));
		verify(employeeRepository, never()).save(any(Employee.class));
		verify(projectRepository, never()).save(any(Project.class));
		verify(interactionRepository, never()).save(any(Interaction.class));
		verify(taskRepository, never()).save(any(Task.class));
		verify(scheduledInteractionRepository, never()).save(any(ScheduledInteraction.class));
	}

	@Test
	void run_whenSeedDataNotPresent_insertsInCorrectForeignKeyOrder() {
		when(userRepository.findByEmail("alice.johnson@psybergate.com"))
				.thenReturn(Optional.empty());

		when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$fakehashvalue");

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
		when(scheduledInteractionRepository.save(any(ScheduledInteraction.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		seedDataLoader.run(applicationArguments);

		// Verify all entity types are saved (FK order is respected)
		// 4 users
		verify(userRepository, times(4)).save(any(User.class));
		// 4 companies
		verify(companyRepository, times(4)).save(any(Company.class));
		// 13 employees
		verify(employeeRepository, times(13)).save(any(Employee.class));
		// 6 projects
		verify(projectRepository, times(6)).save(any(Project.class));
		// 20 interactions
		verify(interactionRepository, times(20)).save(any(Interaction.class));
		// 17 tasks
		verify(taskRepository, times(17)).save(any(Task.class));
		// 26 scheduled interactions
		verify(scheduledInteractionRepository, times(26)).save(any(ScheduledInteraction.class));
	}

	@Test
	void run_idempotency_noSaveCallsWhenSeedDataAlreadyPresent() {
		when(userRepository.findByEmail("alice.johnson@psybergate.com"))
				.thenReturn(Optional.of(new User()));

		seedDataLoader.run(applicationArguments);
		seedDataLoader.run(applicationArguments);

		verify(userRepository, times(2)).findByEmail("alice.johnson@psybergate.com");
		verify(userRepository, never()).save(any(User.class));
		verify(companyRepository, never()).save(any(Company.class));
		verify(employeeRepository, never()).save(any(Employee.class));
		verify(projectRepository, never()).save(any(Project.class));
		verify(interactionRepository, never()).save(any(Interaction.class));
		verify(taskRepository, never()).save(any(Task.class));
		verify(scheduledInteractionRepository, never()).save(any(ScheduledInteraction.class));
	}
}
