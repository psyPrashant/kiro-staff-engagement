package com.psybergate.staff_engagement.employee;

import com.psybergate.staff_engagement.interaction.Interaction;
import com.psybergate.staff_engagement.interaction.InteractionRepository;
import com.psybergate.staff_engagement.scheduling.ScheduledInteraction;
import com.psybergate.staff_engagement.scheduling.ScheduledInteractionRepository;
import com.psybergate.staff_engagement.task.Task;
import com.psybergate.staff_engagement.task.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

	@Mock
	private EmployeeRepository employeeRepository;
	@Mock
	private InteractionRepository interactionRepository;
	@Mock
	private TaskRepository taskRepository;
	@Mock
	private ScheduledInteractionRepository scheduledInteractionRepository;

	private EmployeeService service() {
		return new EmployeeService(employeeRepository, interactionRepository, taskRepository, scheduledInteractionRepository);
	}

	private Employee employee(long id, String name) {
		Employee e = new Employee();
		e.setId(id);
		e.setName(name);
		return e;
	}

	// --- create -------------------------------------------------------------

	@Test
	void create_withManager_savesEmployeeWithTrimmedFieldsAndManager() {
		Employee manager = employee(1L, "Manager Mike");
		when(employeeRepository.findById(1L)).thenReturn(Optional.of(manager));
		when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> {
			Employee e = inv.getArgument(0);
			e.setId(42L);
			return e;
		});

		CreateEmployeeRequest request = new CreateEmployeeRequest(
				"  Jane Doe  ", "  jane@acme.com  ", "  Engineer  ", 1L);

		Employee result = service().create(request);

		assertThat(result.getId()).isEqualTo(42L);

		ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
		verify(employeeRepository).save(captor.capture());
		Employee saved = captor.getValue();
		assertThat(saved.getName()).isEqualTo("Jane Doe");
		assertThat(saved.getEmail()).isEqualTo("jane@acme.com");
		assertThat(saved.getJobTitle()).isEqualTo("Engineer");
		assertThat(saved.getManager()).isSameAs(manager);
	}

	@Test
	void create_withoutManagerId_savesEmployeeWithNullManagerAndJobTitle() {
		when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

		CreateEmployeeRequest request = new CreateEmployeeRequest("John Wick", "john@acme.com", null, null);

		Employee result = service().create(request);

		assertThat(result.getManager()).isNull();
		assertThat(result.getJobTitle()).isNull();
		verify(employeeRepository, never()).findById(any());
	}

	@Test
	void create_nonExistentManagerId_throwsIllegalArgument() {
		when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

		CreateEmployeeRequest request = new CreateEmployeeRequest("Jane Doe", "jane@acme.com", null, 999L);

		assertThatThrownBy(() -> service().create(request))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Manager not found with id: 999");

		verify(employeeRepository, never()).save(any());
	}

	// --- delete ---------------------------------------------------------------

	@Test
	void delete_nonExistentId_throwsEmployeeNotFoundException() {
		when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service().delete(999L))
				.isInstanceOf(EmployeeNotFoundException.class)
				.hasMessageContaining("999");

		verify(employeeRepository, never()).delete(any());
	}

	@Test
	void delete_removesLinkedTasksInteractionsAndScheduledInteractionsAndReassignsReports() {
		Employee target = employee(5L, "Jane Doe");
		when(employeeRepository.findById(5L)).thenReturn(Optional.of(target));

		Interaction interaction = new Interaction();
		interaction.setId(10L);
		when(interactionRepository.findByEmployeeIdOrderByOccurredAtDesc(5L))
				.thenReturn(List.of(interaction));

		Task directTask = new Task();
		directTask.setId(20L);
		Task interactionTask = new Task();
		interactionTask.setId(21L);
		when(taskRepository.findByEmployeeId(5L)).thenReturn(List.of(directTask));
		when(taskRepository.findByInteractionIdIn(List.of(10L))).thenReturn(List.of(interactionTask));

		ScheduledInteraction scheduled = new ScheduledInteraction();
		scheduled.setId(30L);
		when(scheduledInteractionRepository.findByEmployeeId(5L)).thenReturn(List.of(scheduled));

		Employee report = employee(6L, "Report Rachel");
		report.setManager(target);
		when(employeeRepository.findByManagerId(5L)).thenReturn(List.of(report));

		service().delete(5L);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<java.util.Collection<Task>> taskCaptor = ArgumentCaptor.forClass(java.util.Collection.class);
		verify(taskRepository).deleteAll(taskCaptor.capture());
		assertThat(taskCaptor.getValue()).containsExactlyInAnyOrder(directTask, interactionTask);

		verify(scheduledInteractionRepository).deleteAll(List.of(scheduled));
		verify(interactionRepository).deleteAll(List.of(interaction));

		assertThat(report.getManager()).isNull();
		verify(employeeRepository).saveAll(List.of(report));

		verify(employeeRepository).delete(target);
	}

	@Test
	void delete_withNoInteractions_skipsInteractionLinkedTaskLookup() {
		Employee target = employee(5L, "Jane Doe");
		when(employeeRepository.findById(5L)).thenReturn(Optional.of(target));
		when(interactionRepository.findByEmployeeIdOrderByOccurredAtDesc(5L)).thenReturn(List.of());
		when(taskRepository.findByEmployeeId(5L)).thenReturn(List.of());
		when(scheduledInteractionRepository.findByEmployeeId(5L)).thenReturn(List.of());
		when(employeeRepository.findByManagerId(5L)).thenReturn(List.of());

		service().delete(5L);

		verify(taskRepository, never()).findByInteractionIdIn(anyList());
		verify(employeeRepository).delete(target);
	}
}
