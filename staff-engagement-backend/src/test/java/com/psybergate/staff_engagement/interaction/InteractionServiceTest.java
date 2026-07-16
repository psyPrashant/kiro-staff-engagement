package com.psybergate.staff_engagement.interaction;

import com.psybergate.staff_engagement.client.Project;
import com.psybergate.staff_engagement.client.ProjectRepository;
import com.psybergate.staff_engagement.employee.Employee;
import com.psybergate.staff_engagement.employee.EmployeeRepository;
import com.psybergate.staff_engagement.interaction.dto.CreateInteractionRequest;
import com.psybergate.staff_engagement.interaction.dto.UpdateInteractionRequest;
import com.psybergate.staff_engagement.task.Task;
import com.psybergate.staff_engagement.task.TaskRepository;
import com.psybergate.staff_engagement.user.User;
import com.psybergate.staff_engagement.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InteractionServiceTest {

	@Mock
	private InteractionRepository interactionRepository;

	@Mock
	private EmployeeRepository employeeRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private ProjectRepository projectRepository;

	@Mock
	private TaskRepository taskRepository;

	@InjectMocks
	private InteractionService interactionService;

	@Test
	void create_validRequest_savesEntityWithCorrectFields() {
		Employee employee = new Employee();
		employee.setId(1L);
		employee.setName("Jane Doe");

		User conductedBy = new User();
		conductedBy.setId(2L);
		conductedBy.setName("Alice");

		User loggedBy = new User();
		loggedBy.setId(3L);
		loggedBy.setName("Bob");

		Project project = new Project();
		project.setId(10L);
		project.setName("Project X");

		Instant occurredAt = Instant.parse("2024-12-01T10:00:00Z");
		CreateInteractionRequest request = new CreateInteractionRequest(
				1L, 2L, 3L, InteractionType.CHECK_IN, "Weekly check-in", occurredAt, 10L
		);

		when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
		when(userRepository.findById(2L)).thenReturn(Optional.of(conductedBy));
		when(userRepository.findById(3L)).thenReturn(Optional.of(loggedBy));
		when(projectRepository.findById(10L)).thenReturn(Optional.of(project));

		Interaction savedInteraction = new Interaction();
		savedInteraction.setId(42L);
		when(interactionRepository.save(any(Interaction.class))).thenReturn(savedInteraction);

		Interaction result = interactionService.create(request);

		assertNotNull(result);
		assertEquals(42L, result.getId());

		ArgumentCaptor<Interaction> captor = ArgumentCaptor.forClass(Interaction.class);
		verify(interactionRepository).save(captor.capture());

		Interaction captured = captor.getValue();
		assertEquals(employee, captured.getEmployee());
		assertEquals(conductedBy, captured.getConductedBy());
		assertEquals(loggedBy, captured.getLoggedBy());
		assertEquals(project, captured.getProject());
		assertEquals(InteractionType.CHECK_IN, captured.getType());
		assertEquals("Weekly check-in", captured.getNotes());
		assertEquals(occurredAt, captured.getOccurredAt());
	}

	@Test
	void create_nonExistentEmployeeId_throwsIllegalArgument() {
		CreateInteractionRequest request = new CreateInteractionRequest(
				999L, 2L, 3L, InteractionType.CHECK_IN, "Notes", Instant.now(), null
		);

		when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> interactionService.create(request));

		assertTrue(ex.getMessage().contains("Employee not found"));
	}

	@Test
	void create_nonExistentConductedByUserId_throwsIllegalArgument() {
		Employee employee = new Employee();
		employee.setId(1L);

		CreateInteractionRequest request = new CreateInteractionRequest(
				1L, 999L, 3L, InteractionType.MENTORING, "Notes", Instant.now(), null
		);

		when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
		when(userRepository.findById(999L)).thenReturn(Optional.empty());

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> interactionService.create(request));

		assertTrue(ex.getMessage().contains("User not found"));
	}

	@Test
	void create_nonExistentLoggedByUserId_throwsIllegalArgument() {
		Employee employee = new Employee();
		employee.setId(1L);

		User conductedBy = new User();
		conductedBy.setId(2L);

		CreateInteractionRequest request = new CreateInteractionRequest(
				1L, 2L, 999L, InteractionType.CATCH_UP, "Notes", Instant.now(), null
		);

		when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
		when(userRepository.findById(2L)).thenReturn(Optional.of(conductedBy));
		when(userRepository.findById(999L)).thenReturn(Optional.empty());

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> interactionService.create(request));

		assertTrue(ex.getMessage().contains("User not found"));
	}

	@Test
	void create_nonExistentProjectId_throwsIllegalArgument() {
		Employee employee = new Employee();
		employee.setId(1L);

		User conductedBy = new User();
		conductedBy.setId(2L);

		User loggedBy = new User();
		loggedBy.setId(3L);

		CreateInteractionRequest request = new CreateInteractionRequest(
				1L, 2L, 3L, InteractionType.OTHER, "Notes", Instant.now(), 999L
		);

		when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
		when(userRepository.findById(2L)).thenReturn(Optional.of(conductedBy));
		when(userRepository.findById(3L)).thenReturn(Optional.of(loggedBy));
		when(projectRepository.findById(999L)).thenReturn(Optional.empty());

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> interactionService.create(request));

		assertTrue(ex.getMessage().contains("Project not found"));
	}

	@Test
	void create_nullProjectId_savesWithNullProject() {
		Employee employee = new Employee();
		employee.setId(1L);

		User conductedBy = new User();
		conductedBy.setId(2L);

		User loggedBy = new User();
		loggedBy.setId(3L);

		Instant occurredAt = Instant.parse("2024-11-15T14:30:00Z");
		CreateInteractionRequest request = new CreateInteractionRequest(
				1L, 2L, 3L, InteractionType.MENTORING, "Mentoring session", occurredAt, null
		);

		when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
		when(userRepository.findById(2L)).thenReturn(Optional.of(conductedBy));
		when(userRepository.findById(3L)).thenReturn(Optional.of(loggedBy));

		Interaction savedInteraction = new Interaction();
		savedInteraction.setId(7L);
		when(interactionRepository.save(any(Interaction.class))).thenReturn(savedInteraction);

		interactionService.create(request);

		ArgumentCaptor<Interaction> captor = ArgumentCaptor.forClass(Interaction.class);
		verify(interactionRepository).save(captor.capture());

		Interaction captured = captor.getValue();
		assertNull(captured.getProject());
		assertEquals(employee, captured.getEmployee());
		assertEquals(conductedBy, captured.getConductedBy());
		assertEquals(loggedBy, captured.getLoggedBy());
		assertEquals(InteractionType.MENTORING, captured.getType());
		assertEquals("Mentoring session", captured.getNotes());
		assertEquals(occurredAt, captured.getOccurredAt());
	}

	// --- update -------------------------------------------------------------

	@Test
	void update_validRequest_updatesFieldsAndSaves() {
		Interaction existing = new Interaction();
		existing.setId(7L);
		when(interactionRepository.findById(7L)).thenReturn(Optional.of(existing));

		Project project = new Project();
		project.setId(10L);
		when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
		when(interactionRepository.save(any(Interaction.class))).thenAnswer(inv -> inv.getArgument(0));

		Instant occurredAt = Instant.parse("2025-01-10T09:00:00Z");
		UpdateInteractionRequest request = new UpdateInteractionRequest(
				InteractionType.CATCH_UP, "Updated notes", occurredAt, 10L);

		Interaction result = interactionService.update(7L, request);

		assertEquals(InteractionType.CATCH_UP, result.getType());
		assertEquals("Updated notes", result.getNotes());
		assertEquals(occurredAt, result.getOccurredAt());
		assertEquals(project, result.getProject());
	}

	@Test
	void update_nullProjectId_clearsProject() {
		Interaction existing = new Interaction();
		existing.setId(7L);
		existing.setProject(new Project());
		when(interactionRepository.findById(7L)).thenReturn(Optional.of(existing));
		when(interactionRepository.save(any(Interaction.class))).thenAnswer(inv -> inv.getArgument(0));

		UpdateInteractionRequest request = new UpdateInteractionRequest(
				InteractionType.OTHER, "Notes", Instant.now(), null);

		Interaction result = interactionService.update(7L, request);

		assertNull(result.getProject());
	}

	@Test
	void update_nonExistentId_throwsInteractionNotFound() {
		when(interactionRepository.findById(999L)).thenReturn(Optional.empty());

		UpdateInteractionRequest request = new UpdateInteractionRequest(
				InteractionType.CHECK_IN, "Notes", Instant.now(), null);

		assertThrows(InteractionNotFoundException.class, () -> interactionService.update(999L, request));
		verify(interactionRepository, never()).save(any());
	}

	@Test
	void update_nonExistentProjectId_throwsIllegalArgument() {
		Interaction existing = new Interaction();
		existing.setId(7L);
		when(interactionRepository.findById(7L)).thenReturn(Optional.of(existing));
		when(projectRepository.findById(999L)).thenReturn(Optional.empty());

		UpdateInteractionRequest request = new UpdateInteractionRequest(
				InteractionType.CHECK_IN, "Notes", Instant.now(), 999L);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> interactionService.update(7L, request));
		assertTrue(ex.getMessage().contains("Project not found"));
		verify(interactionRepository, never()).save(any());
	}

	// --- delete ---------------------------------------------------------------

	@Test
	void delete_existingId_detachesLinkedTasksAndDeletesInteraction() {
		Interaction existing = new Interaction();
		existing.setId(7L);
		when(interactionRepository.findById(7L)).thenReturn(Optional.of(existing));

		Task linkedTask = new Task();
		linkedTask.setId(50L);
		linkedTask.setInteraction(existing);
		when(taskRepository.findByInteractionIdIn(List.of(7L))).thenReturn(List.of(linkedTask));

		interactionService.delete(7L);

		assertNull(linkedTask.getInteraction());
		verify(taskRepository).saveAll(List.of(linkedTask));
		verify(interactionRepository).delete(existing);
	}

	@Test
	void delete_nonExistentId_throwsInteractionNotFound() {
		when(interactionRepository.findById(999L)).thenReturn(Optional.empty());

		assertThrows(InteractionNotFoundException.class, () -> interactionService.delete(999L));
		verify(interactionRepository, never()).delete(any());
	}
}
