package com.psybergate.staff_engagement.interaction;

import com.psybergate.staff_engagement.client.Project;
import com.psybergate.staff_engagement.client.ProjectRepository;
import com.psybergate.staff_engagement.employee.Employee;
import com.psybergate.staff_engagement.employee.EmployeeRepository;
import com.psybergate.staff_engagement.interaction.dto.CreateInteractionRequest;
import com.psybergate.staff_engagement.user.User;
import com.psybergate.staff_engagement.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
}
