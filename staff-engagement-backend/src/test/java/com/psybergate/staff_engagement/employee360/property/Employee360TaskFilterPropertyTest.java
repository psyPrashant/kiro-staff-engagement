package com.psybergate.staff_engagement.employee360.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.psybergate.staff_engagement.employee.domain.Employee;
import com.psybergate.staff_engagement.employee.domain.EmployeeRepository;
import com.psybergate.staff_engagement.employee360.dto.Employee360Response;
import com.psybergate.staff_engagement.employee360.dto.TaskDto;
import com.psybergate.staff_engagement.employee360.service.Employee360Service;
import com.psybergate.staff_engagement.employee360.service.Employee360ServiceImpl;
import com.psybergate.staff_engagement.interaction.domain.Interaction;
import com.psybergate.staff_engagement.interaction.domain.InteractionRepository;
import com.psybergate.staff_engagement.interaction.domain.InteractionType;
import com.psybergate.staff_engagement.scheduling.service.NextScheduledInteractionService;
import com.psybergate.staff_engagement.task.domain.Task;
import com.psybergate.staff_engagement.task.domain.TaskRepository;
import com.psybergate.staff_engagement.task.domain.TaskStatus;
import com.psybergate.staff_engagement.user.domain.User;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import net.jqwik.api.*;
import org.mockito.ArgumentCaptor;

/**
 * Property-based test: Only OPEN tasks are included in the Employee 360 response.
 *
 * Validates: Requirements 1.3
 */
@Tag("Feature: employee-360-view, Property 3: Only OPEN tasks are included")
class Employee360TaskFilterPropertyTest {

	/**
	 * Property 3: Only OPEN tasks are included
	 *
	 * For any set of tasks linked to an employee's interactions, the response SHALL
	 * include only those tasks where status == OPEN. No task with status DONE shall
	 * appear in the openTasks list.
	 *
	 * Validates: Requirements 1.3
	 */
	@Property(tries = 100)
	@Tag("Feature: employee-360-view, Property 3: Only OPEN tasks are included")
	void responseContainsOnlyOpenTasks(@ForAll("mixedStatusTasks") List<Task> allTasks) {
		// Fresh mocks per property invocation
		EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
		InteractionRepository interactionRepository = mock(InteractionRepository.class);
		TaskRepository taskRepository = mock(TaskRepository.class);
		NextScheduledInteractionService nextScheduledInteractionService = mock(NextScheduledInteractionService.class);
		Employee360Service service = new Employee360ServiceImpl(
				employeeRepository, interactionRepository, taskRepository, nextScheduledInteractionService
		);

		// Arrange: set up a valid employee and interaction
		Employee employee = createEmployee(1L);
		Interaction interaction = createInteraction(1L, employee);

		when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
		when(interactionRepository.findByEmployeeIdOrderByOccurredAtDesc(1L))
				.thenReturn(List.of(interaction));

		// Filter to only OPEN tasks — simulating what the repository query does
		List<Task> openTasks = allTasks.stream()
				.filter(t -> t.getStatus() == TaskStatus.OPEN)
				.toList();

		when(taskRepository.findByInteractionIdInAndStatus(eq(List.of(1L)), eq(TaskStatus.OPEN)))
				.thenReturn(openTasks);

		// Act
		Employee360Response response = service.getEmployee360(1L);

		// Assert: the service passes TaskStatus.OPEN to the repository
		ArgumentCaptor<TaskStatus> statusCaptor = ArgumentCaptor.forClass(TaskStatus.class);
		verify(taskRepository).findByInteractionIdInAndStatus(eq(List.of(1L)), statusCaptor.capture());
		assertThat(statusCaptor.getValue()).isEqualTo(TaskStatus.OPEN);

		// Assert: response openTasks count matches the number of OPEN tasks
		assertThat(response.openTasks()).hasSize(openTasks.size());

		// Assert: no DONE task title appears in the response — only OPEN task titles are present
		List<String> openTaskTitles = openTasks.stream().map(Task::getTitle).toList();
		List<String> responseTitles = response.openTasks().stream().map(TaskDto::title).toList();
		assertThat(responseTitles).containsExactlyInAnyOrderElementsOf(openTaskTitles);
	}

	@Provide
	Arbitrary<List<Task>> mixedStatusTasks() {
		Arbitrary<Task> taskArbitrary = Combinators.combine(
				Arbitraries.longs().between(1L, 1000L),
				Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50),
				Arbitraries.strings().alpha().ofMinLength(0).ofMaxLength(200),
				Arbitraries.of(TaskStatus.OPEN, TaskStatus.DONE),
				Arbitraries.of(true, false)
		).as((id, title, description, status, hasDueDate) -> {
			Task task = new Task();
			task.setId(id);
			task.setTitle(title);
			task.setDescription(description);
			task.setStatus(status);
			task.setDueDate(hasDueDate ? LocalDate.now().plusDays(id % 30) : null);
			task.setCreatedAt(Instant.now());
			return task;
		});

		return taskArbitrary.list().ofMinSize(0).ofMaxSize(10);
	}

	private Employee createEmployee(Long id) {
		Employee employee = new Employee();
		employee.setId(id);
		employee.setName("Test Employee");
		employee.setEmail("test@example.com");
		employee.setJobTitle("Developer");
		employee.setCreatedAt(Instant.now());
		return employee;
	}

	private Interaction createInteraction(Long id, Employee employee) {
		Interaction interaction = new Interaction();
		interaction.setId(id);
		interaction.setEmployee(employee);
		interaction.setType(InteractionType.CHECK_IN);
		interaction.setNotes("Test notes");
		interaction.setOccurredAt(Instant.now());
		interaction.setCreatedAt(Instant.now());

		User conductedBy = new User();
		conductedBy.setId(1L);
		conductedBy.setName("Conductor");
		conductedBy.setEmail("conductor@example.com");
		interaction.setConductedBy(conductedBy);

		User loggedBy = new User();
		loggedBy.setId(2L);
		loggedBy.setName("Logger");
		loggedBy.setEmail("logger@example.com");
		interaction.setLoggedBy(loggedBy);

		return interaction;
	}
}
