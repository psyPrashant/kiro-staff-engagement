package com.psybergate.staff_engagement.task;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

	@Mock
	private TaskRepository taskRepository;

	@Mock
	private InteractionRepository interactionRepository;

	@Mock
	private UserRepository userRepository;

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
				"Follow up on career plan", "Schedule meeting to discuss progress", 42L, dueDate, 2L
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
				"Some task", "Description", 999L, null, null
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
				"Some task", "Description", 42L, null, 999L
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
				"Standalone task", "No interaction or user", null, null, null
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
}
