package com.psybergate.staff_engagement.employee360;

import com.psybergate.staff_engagement.client.Project;
import com.psybergate.staff_engagement.employee.Employee;
import com.psybergate.staff_engagement.employee.EmployeeRepository;
import com.psybergate.staff_engagement.interaction.Interaction;
import com.psybergate.staff_engagement.interaction.InteractionRepository;
import com.psybergate.staff_engagement.task.Task;
import com.psybergate.staff_engagement.task.TaskRepository;
import com.psybergate.staff_engagement.task.TaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class Employee360Service {

	private final EmployeeRepository employeeRepository;
	private final InteractionRepository interactionRepository;
	private final TaskRepository taskRepository;

	@Transactional(readOnly = true)
	public Employee360Response getEmployee360(Long employeeId) {
		Employee employee = employeeRepository.findById(employeeId)
			.orElseThrow(() -> new Employee360NotFoundException(employeeId));

		List<Interaction> interactions = interactionRepository
			.findByEmployeeIdOrderByOccurredAtDesc(employeeId);

		List<Long> interactionIds = interactions.stream()
			.map(Interaction::getId).toList();

		List<Task> openTasks = interactionIds.isEmpty()
			? List.of()
			: taskRepository.findByInteractionIdInAndStatus(interactionIds, TaskStatus.OPEN);

		return buildResponse(employee, interactions, openTasks);
	}

	private Employee360Response buildResponse(Employee employee, List<Interaction> interactions, List<Task> openTasks) {
		ProfileDto profile = mapProfile(employee);
		List<InteractionDto> interactionDtos = interactions.stream()
			.map(this::mapInteraction)
			.toList();
		List<TaskDto> taskDtos = openTasks.stream()
			.map(this::mapTask)
			.toList();

		return new Employee360Response(profile, interactionDtos, taskDtos);
	}

	private ProfileDto mapProfile(Employee employee) {
		String managerName = employee.getManager() != null
			? employee.getManager().getName()
			: null;

		return new ProfileDto(
			employee.getId(),
			employee.getName(),
			employee.getEmail(),
			employee.getJobTitle(),
			managerName
		);
	}

	private InteractionDto mapInteraction(Interaction interaction) {
		ProjectContextDto projectContext = null;
		Project project = interaction.getProject();
		if (project != null) {
			projectContext = new ProjectContextDto(
				project.getName(),
				project.getCompany().getName()
			);
		}

		return new InteractionDto(
			interaction.getId(),
			interaction.getType().name(),
			interaction.getOccurredAt(),
			interaction.getConductedBy().getName(),
			interaction.getNotes(),
			projectContext
		);
	}

	private TaskDto mapTask(Task task) {
		String assignedUserName = task.getAssignedUser() != null
			? task.getAssignedUser().getName()
			: null;

		return new TaskDto(
			task.getId(),
			task.getTitle(),
			task.getDueDate(),
			assignedUserName
		);
	}
}
