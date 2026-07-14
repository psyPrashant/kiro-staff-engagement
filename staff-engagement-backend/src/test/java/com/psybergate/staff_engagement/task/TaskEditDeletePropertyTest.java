package com.psybergate.staff_engagement.task;

import com.psybergate.staff_engagement.employee.Employee;
import com.psybergate.staff_engagement.employee.EmployeeRepository;
import com.psybergate.staff_engagement.interaction.Interaction;
import com.psybergate.staff_engagement.interaction.InteractionRepository;
import com.psybergate.staff_engagement.task.dto.TaskResponse;
import com.psybergate.staff_engagement.task.dto.UpdateTaskRequest;
import com.psybergate.staff_engagement.user.User;
import com.psybergate.staff_engagement.user.UserRepository;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Property-based tests for the Task Edit & Delete API using jqwik + Mockito.
 *
 * Feature: task-edit-delete-api
 */
class TaskEditDeletePropertyTest {

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
		taskService = new TaskService(taskRepository, interactionRepository, userRepository, employeeRepository);
	}

	/**
	 * Property 1: Update round-trip persists submitted fields
	 *
	 * For any existing Task and any valid UpdateTaskRequest, update returns a Task whose
	 * title/description/dueDate equal the request, and TaskResponse.from(result) reflects
	 * the updated entity state.
	 *
	 * Validates: Requirements 1.1, 1.8, 6.1
	 */
	@Property(tries = 100)
	@Tag("Feature: task-edit-delete-api, Property 1: Update round-trip persists submitted fields")
	void updateRoundTripPersistsFields(
			@ForAll("validTitles") String title,
			@ForAll("descriptions") String description,
			@ForAll("nullableDueDates") LocalDate dueDate,
			@ForAll("statuses") TaskStatus currentStatus) {
		Task existing = new Task();
		existing.setId(1L);
		existing.setStatus(currentStatus);
		existing.setCreatedAt(Instant.now());

		when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
		when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

		UpdateTaskRequest request = new UpdateTaskRequest(
				title, description, null, null, dueDate, null, null);

		Task result = taskService.update(1L, request);

		assertThat(result.getTitle()).isEqualTo(title);
		assertThat(result.getDescription()).isEqualTo(description);
		assertThat(result.getDueDate()).isEqualTo(dueDate);

		TaskResponse response = TaskResponse.from(result);
		assertThat(response.title()).isEqualTo(title);
		assertThat(response.description()).isEqualTo(description);
		assertThat(response.dueDate()).isEqualTo(dueDate);
		assertThat(response.status()).isEqualTo(result.getStatus().name());
		assertThat(response.employeeId()).isNull();
		assertThat(response.assignedUserId()).isNull();
		assertThat(response.interactionId()).isNull();
	}

	/**
	 * Property 2: Association fields are set or cleared based on request nullability
	 *
	 * For any existing Task with arbitrary prior associations and any UpdateTaskRequest,
	 * each association is non-null (resolved entity) when the request id is non-null, and
	 * null when the request id is null — regardless of prior state.
	 *
	 * Validates: Requirements 1.4, 1.5, 1.6, 1.7, 6.2, 6.3
	 */
	@Property(tries = 100)
	@Tag("Feature: task-edit-delete-api, Property 2: Association fields are set or cleared based on request nullability")
	void associationsSetOrClearedByRequestNullability(
			@ForAll boolean priorEmployee, @ForAll boolean priorUser, @ForAll boolean priorInteraction,
			@ForAll boolean reqEmployee, @ForAll boolean reqUser, @ForAll boolean reqInteraction) {
		Task existing = new Task();
		existing.setId(1L);
		existing.setStatus(TaskStatus.OPEN);
		if (priorEmployee) {
			Employee e = new Employee();
			e.setId(100L);
			existing.setEmployee(e);
		}
		if (priorUser) {
			User u = new User();
			u.setId(200L);
			existing.setAssignedUser(u);
		}
		if (priorInteraction) {
			Interaction i = new Interaction();
			i.setId(300L);
			existing.setInteraction(i);
		}

		when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
		when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

		Long employeeId = null;
		Long userId = null;
		Long interactionId = null;
		if (reqEmployee) {
			employeeId = 5L;
			Employee ne = new Employee();
			ne.setId(5L);
			when(employeeRepository.findById(5L)).thenReturn(Optional.of(ne));
		}
		if (reqUser) {
			userId = 6L;
			User nu = new User();
			nu.setId(6L);
			when(userRepository.findById(6L)).thenReturn(Optional.of(nu));
		}
		if (reqInteraction) {
			interactionId = 7L;
			Interaction ni = new Interaction();
			ni.setId(7L);
			when(interactionRepository.findById(7L)).thenReturn(Optional.of(ni));
		}

		UpdateTaskRequest request = new UpdateTaskRequest(
				"Title", null, interactionId, employeeId, null, userId, null);

		Task result = taskService.update(1L, request);

		if (reqEmployee) {
			assertThat(result.getEmployee()).isNotNull();
			assertThat(result.getEmployee().getId()).isEqualTo(5L);
		} else {
			assertThat(result.getEmployee()).isNull();
		}
		if (reqUser) {
			assertThat(result.getAssignedUser()).isNotNull();
			assertThat(result.getAssignedUser().getId()).isEqualTo(6L);
		} else {
			assertThat(result.getAssignedUser()).isNull();
		}
		if (reqInteraction) {
			assertThat(result.getInteraction()).isNotNull();
			assertThat(result.getInteraction().getId()).isEqualTo(7L);
		} else {
			assertThat(result.getInteraction()).isNull();
		}
	}

	/**
	 * Property 3: Status is set from the request or retained when omitted
	 *
	 * For any existing Task with an arbitrary current status and any request status in
	 * {OPEN, DONE, null}, the resulting status equals the request status when non-null,
	 * else the prior status.
	 *
	 * Validates: Requirements 2.2, 2.3, 2.4
	 */
	@Property(tries = 100)
	@Tag("Feature: task-edit-delete-api, Property 3: Status is set from the request or retained when omitted")
	void statusSetFromRequestOrRetained(
			@ForAll("statuses") TaskStatus currentStatus,
			@ForAll("nullableStatuses") TaskStatus requestStatus) {
		Task existing = new Task();
		existing.setId(1L);
		existing.setStatus(currentStatus);

		when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
		when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

		UpdateTaskRequest request = new UpdateTaskRequest(
				"Title", null, null, null, null, null, requestStatus);

		Task result = taskService.update(1L, request);

		TaskStatus expected = requestStatus != null ? requestStatus : currentStatus;
		assertThat(result.getStatus()).isEqualTo(expected);
	}

	/**
	 * Property 4: Invalid foreign key references are rejected without persisting
	 *
	 * For any existing Task and a request where exactly one FK id does not resolve,
	 * update throws IllegalArgumentException and taskRepository.save is never called.
	 *
	 * Validates: Requirements 5.3
	 */
	@Property(tries = 100)
	@Tag("Feature: task-edit-delete-api, Property 4: Invalid foreign key references are rejected without persisting")
	void invalidForeignKeyRejectedWithoutPersisting(@ForAll("fkSlot") int slot) {
		Task existing = new Task();
		existing.setId(1L);
		existing.setStatus(TaskStatus.OPEN);
		when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));

		Long interactionId = null;
		Long employeeId = null;
		Long userId = null;
		long badId = 999L;
		switch (slot) {
			case 0 -> {
				interactionId = badId;
				when(interactionRepository.findById(badId)).thenReturn(Optional.empty());
			}
			case 1 -> {
				userId = badId;
				when(userRepository.findById(badId)).thenReturn(Optional.empty());
			}
			default -> {
				employeeId = badId;
				when(employeeRepository.findById(badId)).thenReturn(Optional.empty());
			}
		}

		UpdateTaskRequest request = new UpdateTaskRequest(
				"Title", null, interactionId, employeeId, null, userId, null);

		assertThatThrownBy(() -> taskService.update(1L, request))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("not found with id: " + badId);

		verify(taskRepository, never()).save(any());
	}

	/**
	 * Property 5: Update and delete of a non-existent task throw TaskNotFoundException
	 *
	 * For any task id absent from the repository, both update and delete throw
	 * TaskNotFoundException.
	 *
	 * Validates: Requirements 4.1, 4.2
	 */
	@Property(tries = 100)
	@Tag("Feature: task-edit-delete-api, Property 5: Update and delete of a non-existent task return 404")
	void updateAndDeleteOfMissingTaskThrowNotFound(@ForAll("absentIds") Long id) {
		when(taskRepository.findById(id)).thenReturn(Optional.empty());

		UpdateTaskRequest request = new UpdateTaskRequest(
				"Title", null, null, null, null, null, null);

		assertThatThrownBy(() -> taskService.update(id, request))
				.isInstanceOf(TaskNotFoundException.class);
		assertThatThrownBy(() -> taskService.delete(id))
				.isInstanceOf(TaskNotFoundException.class);
	}

	/**
	 * Property 6: Delete removes the task from the repository
	 *
	 * For any existing Task id, after delete, taskRepository.findById returns empty.
	 *
	 * Validates: Requirements 3.1
	 */
	@Property(tries = 100)
	@Tag("Feature: task-edit-delete-api, Property 6: Delete removes the task from the repository")
	void deleteRemovesTaskFromRepository(@ForAll("absentIds") Long id) {
		Task existing = new Task();
		existing.setId(id);
		boolean[] deleted = {false};

		when(taskRepository.findById(id))
				.thenAnswer(inv -> deleted[0] ? Optional.empty() : Optional.of(existing));
		org.mockito.Mockito.doAnswer(inv -> {
			deleted[0] = true;
			return null;
		}).when(taskRepository).delete(existing);

		taskService.delete(id);

		assertThat(taskRepository.findById(id)).isEmpty();
	}

	// --- Generators ---

	@Provide
	Arbitrary<String> validTitles() {
		return Arbitraries.strings()
				.withCharRange('a', 'z')
				.ofMinLength(1)
				.ofMaxLength(255)
				.filter(s -> !s.isBlank());
	}

	@Provide
	Arbitrary<String> descriptions() {
		Arbitrary<String> text = Arbitraries.strings()
				.withCharRange('a', 'z')
				.ofMinLength(0)
				.ofMaxLength(2000);
		return Arbitraries.oneOf(Arbitraries.just(null), text);
	}

	@Provide
	Arbitrary<LocalDate> nullableDueDates() {
		Arbitrary<LocalDate> dates = Arbitraries.integers().between(-3650, 3650)
				.map(days -> LocalDate.of(2025, 1, 1).plusDays(days));
		return Arbitraries.oneOf(Arbitraries.just(null), dates);
	}

	@Provide
	Arbitrary<TaskStatus> statuses() {
		return Arbitraries.of(TaskStatus.OPEN, TaskStatus.DONE);
	}

	@Provide
	Arbitrary<TaskStatus> nullableStatuses() {
		return Arbitraries.of(TaskStatus.OPEN, TaskStatus.DONE, null);
	}

	@Provide
	Arbitrary<Integer> fkSlot() {
		return Arbitraries.integers().between(0, 2);
	}

	@Provide
	Arbitrary<Long> absentIds() {
		return Arbitraries.longs().between(1L, 1_000_000L);
	}
}
