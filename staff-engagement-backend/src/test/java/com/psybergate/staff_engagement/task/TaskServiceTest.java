package com.psybergate.staff_engagement.task;

import com.psybergate.staff_engagement.employee.Employee;
import com.psybergate.staff_engagement.employee.EmployeeRepository;
import com.psybergate.staff_engagement.interaction.Interaction;
import com.psybergate.staff_engagement.interaction.InteractionRepository;
import com.psybergate.staff_engagement.task.dto.CreateTaskRequest;
import com.psybergate.staff_engagement.user.User;
import com.psybergate.staff_engagement.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

	@Mock
	private TaskRepository taskRepository;

	@Mock
	private InteractionRepository interactionRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private EmployeeRepository employeeRepository;

	@InjectMocks
	private TaskService taskService;

	@Test
	void create_validRequest_savesEntityWithStatusOpen() {
		Interaction interaction = new Interaction();
		interaction.setId(42L);

		User assignedUser = new User();
		assignedUser.setId(2L);
		assignedUser.setName("Alice");

		LocalDate dueDate = LocalDate.of(2025, 1, 15);
		CreateTaskRequest request = new CreateTaskRequest(
				"Follow up on career plan", "Schedule meeting to discuss progress", 42L, null, dueDate, 2L
		);

		when(interactionRepository.findById(42L)).thenReturn(Optional.of(interaction));
		when(userRepository.findById(2L)).thenReturn(Optional.of(assignedUser));

		Task savedTask = new Task();
		savedTask.setId(7L);
		when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

		Task result = taskService.create(request);

		assertNotNull(result);
		assertEquals(7L, result.getId());

		ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
		verify(taskRepository).save(captor.capture());

		Task captured = captor.getValue();
		assertEquals(TaskStatus.OPEN, captured.getStatus());
		assertEquals("Follow up on career plan", captured.getTitle());
		assertEquals("Schedule meeting to discuss progress", captured.getDescription());
		assertEquals(interaction, captured.getInteraction());
		assertEquals(assignedUser, captured.getAssignedUser());
		assertEquals(dueDate, captured.getDueDate());
	}

	@Test
	void create_nonExistentInteractionId_throwsIllegalArgument() {
		CreateTaskRequest request = new CreateTaskRequest(
				"Some task", "Description", 999L, null, null, null
		);

		when(interactionRepository.findById(999L)).thenReturn(Optional.empty());

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> taskService.create(request));

		assertTrue(ex.getMessage().contains("Interaction not found"));
	}

	@Test
	void create_nonExistentAssignedUserId_throwsIllegalArgument() {
		Interaction interaction = new Interaction();
		interaction.setId(42L);

		CreateTaskRequest request = new CreateTaskRequest(
				"Some task", "Description", 42L, null, null, 999L
		);

		when(interactionRepository.findById(42L)).thenReturn(Optional.of(interaction));
		when(userRepository.findById(999L)).thenReturn(Optional.empty());

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> taskService.create(request));

		assertTrue(ex.getMessage().contains("User not found"));
	}

	@Test
	void create_nullInteractionIdAndAssignedUserId_savesSuccessfully() {
		CreateTaskRequest request = new CreateTaskRequest(
				"Standalone task", "No interaction or user", null, null, null, null
		);

		Task savedTask = new Task();
		savedTask.setId(10L);
		when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

		taskService.create(request);

		ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
		verify(taskRepository).save(captor.capture());

		Task captured = captor.getValue();
		assertNull(captured.getInteraction());
		assertNull(captured.getAssignedUser());
		assertEquals("Standalone task", captured.getTitle());
		assertEquals("No interaction or user", captured.getDescription());
		assertEquals(TaskStatus.OPEN, captured.getStatus());
	}

	@Test
	void create_validEmployeeId_resolvesAndSetsEmployeeOnTask() {
		Employee employee = new Employee();
		employee.setId(5L);
		employee.setName("Jane Doe");
		employee.setEmail("jane@example.com");

		CreateTaskRequest request = new CreateTaskRequest(
				"Employee task", "Linked to employee", null, 5L, null, null
		);

		when(employeeRepository.findById(5L)).thenReturn(Optional.of(employee));

		Task savedTask = new Task();
		savedTask.setId(20L);
		when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

		taskService.create(request);

		ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
		verify(taskRepository).save(captor.capture());

		Task captured = captor.getValue();
		assertNotNull(captured.getEmployee());
		assertEquals(5L, captured.getEmployee().getId());
		assertEquals("Jane Doe", captured.getEmployee().getName());
		assertEquals(TaskStatus.OPEN, captured.getStatus());
	}

	@Test
	void create_invalidEmployeeId_throwsIllegalArgument() {
		CreateTaskRequest request = new CreateTaskRequest(
				"Bad employee task", "Invalid employee", null, 999L, null, null
		);

		when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> taskService.create(request));

		assertTrue(ex.getMessage().contains("Employee not found"));
		verify(taskRepository, never()).save(any());
	}

	@Test
	void create_bothEmployeeIdAndInteractionId_resolvesBoth() {
		Employee employee = new Employee();
		employee.setId(3L);
		employee.setName("Bob Smith");
		employee.setEmail("bob@example.com");

		Interaction interaction = new Interaction();
		interaction.setId(10L);

		CreateTaskRequest request = new CreateTaskRequest(
				"Dual link task", "Both employee and interaction", 10L, 3L, null, null
		);

		when(employeeRepository.findById(3L)).thenReturn(Optional.of(employee));
		when(interactionRepository.findById(10L)).thenReturn(Optional.of(interaction));

		Task savedTask = new Task();
		savedTask.setId(30L);
		when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

		taskService.create(request);

		ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
		verify(taskRepository).save(captor.capture());

		Task captured = captor.getValue();
		assertNotNull(captured.getEmployee());
		assertEquals(3L, captured.getEmployee().getId());
		assertNotNull(captured.getInteraction());
		assertEquals(10L, captured.getInteraction().getId());
		assertEquals(TaskStatus.OPEN, captured.getStatus());
	}

	@Test
	void create_nullEmployeeIdWithValidInteractionId_preservesExistingBehavior() {
		Interaction interaction = new Interaction();
		interaction.setId(15L);

		CreateTaskRequest request = new CreateTaskRequest(
				"Interaction-only task", "No employee link", 15L, null, null, null
		);

		when(interactionRepository.findById(15L)).thenReturn(Optional.of(interaction));

		Task savedTask = new Task();
		savedTask.setId(40L);
		when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

		taskService.create(request);

		ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
		verify(taskRepository).save(captor.capture());

		Task captured = captor.getValue();
		assertNull(captured.getEmployee());
		assertNotNull(captured.getInteraction());
		assertEquals(15L, captured.getInteraction().getId());
		assertEquals(TaskStatus.OPEN, captured.getStatus());
	}

	@Test
	void create_nullEmployeeIdAndNullInteractionId_createsStandaloneTask() {
		CreateTaskRequest request = new CreateTaskRequest(
				"Standalone", "No links at all", null, null, LocalDate.of(2025, 6, 1), null
		);

		Task savedTask = new Task();
		savedTask.setId(50L);
		when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

		taskService.create(request);

		ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
		verify(taskRepository).save(captor.capture());

		Task captured = captor.getValue();
		assertNull(captured.getEmployee());
		assertNull(captured.getInteraction());
		assertNull(captured.getAssignedUser());
		assertEquals("Standalone", captured.getTitle());
		assertEquals(LocalDate.of(2025, 6, 1), captured.getDueDate());
		assertEquals(TaskStatus.OPEN, captured.getStatus());
	}
}
