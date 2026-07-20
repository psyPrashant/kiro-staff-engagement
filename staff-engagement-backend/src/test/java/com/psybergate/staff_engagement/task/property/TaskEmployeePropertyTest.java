package com.psybergate.staff_engagement.task.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.psybergate.staff_engagement.employee.domain.Employee;
import com.psybergate.staff_engagement.employee.domain.EmployeeRepository;
import com.psybergate.staff_engagement.interaction.domain.Interaction;
import com.psybergate.staff_engagement.interaction.domain.InteractionRepository;
import com.psybergate.staff_engagement.task.domain.Task;
import com.psybergate.staff_engagement.task.domain.TaskRepository;
import com.psybergate.staff_engagement.task.domain.TaskStatus;
import com.psybergate.staff_engagement.task.dto.CreateTaskRequest;
import com.psybergate.staff_engagement.task.dto.TaskResponse;
import com.psybergate.staff_engagement.task.service.TaskService;
import com.psybergate.staff_engagement.task.service.TaskServiceImpl;
import com.psybergate.staff_engagement.user.domain.UserRepository;
import java.util.Optional;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Property-based tests for the Task-Employee Link feature using jqwik + Mockito.
 *
 * Validates: Requirements 2.3, 3.2, 4.1
 */
class TaskEmployeePropertyTest {

	@Mock
	private TaskRepository taskRepository;

	@Mock
	private InteractionRepository interactionRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private EmployeeRepository employeeRepository;

	private TaskService taskService;

	@BeforeProperty
	void setUp() {
		MockitoAnnotations.openMocks(this);
		taskService = new TaskServiceImpl(taskRepository, interactionRepository, userRepository, employeeRepository);
	}

	/**
	 * Property 1: Employee resolution round-trip
	 *
	 * For any valid employee that exists in the repository, creating a task with that
	 * employee's ID and then mapping the result to a TaskResponse SHALL yield a response
	 * where employeeId equals the provided ID and employeeName equals the employee's name.
	 *
	 * Validates: Requirements 2.3, 3.2, 4.1
	 */
	@Property(tries = 100)
	@Tag("Feature: task-employee-link, Property 1: Employee resolution round-trip")
	void employeeResolutionRoundTrip(@ForAll("validEmployees") Employee employee) {
		// Arrange: mock repository to return the generated employee
		when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
		when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// Act: create a task with the employee's ID
		CreateTaskRequest request = new CreateTaskRequest(
			"Test Task",
			"Description",
			null,
			employee.getId(),
			null,
			null
		);
		Task savedTask = taskService.create(request);
		TaskResponse response = TaskResponse.from(savedTask);

		// Assert: round-trip preserves employee identity
		assertThat(response.employeeId()).isEqualTo(employee.getId());
		assertThat(response.employeeName()).isEqualTo(employee.getName());
	}

	/**
	 * Property 5: Standalone task creation
	 *
	 * For any valid title string (non-blank, max 255 chars), creating a task with only
	 * that title (null interactionId, null employeeId) SHALL produce a task with status
	 * OPEN, null employee, and null interaction.
	 *
	 * Validates: Requirements 3.4, 5.2
	 */
	@Property(tries = 100)
	@Tag("Feature: task-employee-link, Property 5: Standalone task creation")
	void standaloneTaskCreation(@ForAll("validTitles") String title) {
		when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

		CreateTaskRequest request = new CreateTaskRequest(title, null, null, null, null, null);
		Task result = taskService.create(request);

		assertThat(result.getStatus()).isEqualTo(TaskStatus.OPEN);
		assertThat(result.getEmployee()).isNull();
		assertThat(result.getInteraction()).isNull();
		assertThat(result.getAssignedUser()).isNull();
		assertThat(result.getTitle()).isEqualTo(title);
	}

	/**
	 * Generates random valid title strings:
	 * - 1-255 characters
	 * - Non-blank (lowercase alphabetic)
	 */
	@Provide
	Arbitrary<String> validTitles() {
		return Arbitraries.strings()
			.withCharRange('a', 'z')
			.ofMinLength(1)
			.ofMaxLength(255)
			.filter(s -> !s.isBlank());
	}

	/**
	 * Generates random valid Employee entities with:
	 * - Random Long id between 1 and 10000
	 * - Random name 1-50 alphanumeric characters
	 * - Random email address
	 */
	@Provide
	Arbitrary<Employee> validEmployees() {
		Arbitrary<Long> ids = Arbitraries.longs().between(1L, 10000L);
		Arbitrary<String> names = Arbitraries.strings()
			.withCharRange('a', 'z')
			.ofMinLength(1)
			.ofMaxLength(50);
		Arbitrary<String> emails = Arbitraries.strings()
			.withCharRange('a', 'z')
			.ofMinLength(3)
			.ofMaxLength(20)
			.map(s -> s + "@example.com");

		return Combinators.combine(ids, names, emails).as((id, name, email) -> {
			Employee emp = new Employee();
			emp.setId(id);
			emp.setName(name);
			emp.setEmail(email);
			return emp;
		});
	}

	/**
	 * Generates random valid Interaction entities with:
	 * - Random Long id between 1 and 10000
	 * - A linked Employee
	 */
	@Provide
	Arbitrary<Interaction> validInteractions() {
		Arbitrary<Long> ids = Arbitraries.longs().between(1L, 10000L);

		return ids.map(id -> {
			Interaction interaction = new Interaction();
			interaction.setId(id);
			return interaction;
		});
	}

	/**
	 * Property 6: Dual association
	 *
	 * For any valid employeeId and valid interactionId that both exist in the repository,
	 * creating a task with both SHALL produce a task where both the employee and the
	 * interaction are associated, and neither is null.
	 *
	 * Validates: Requirements 3.5
	 */
	@Property(tries = 100)
	@Tag("Feature: task-employee-link, Property 6: Dual association")
	void dualAssociation(
		@ForAll("validEmployees") Employee employee,
		@ForAll("validInteractions") Interaction interaction
	) {
		// Arrange: mock repositories to return both entities
		when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
		when(interactionRepository.findById(interaction.getId())).thenReturn(Optional.of(interaction));
		when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

		// Act: create a task with both employeeId and interactionId
		CreateTaskRequest request = new CreateTaskRequest(
			"Dual task", "desc", interaction.getId(), employee.getId(), null, null
		);

		Task result = taskService.create(request);

		// Assert: both associations are present
		assertThat(result.getEmployee()).isNotNull();
		assertThat(result.getEmployee().getId()).isEqualTo(employee.getId());
		assertThat(result.getInteraction()).isNotNull();
		assertThat(result.getInteraction().getId()).isEqualTo(interaction.getId());
		assertThat(result.getStatus()).isEqualTo(TaskStatus.OPEN);
	}
}
