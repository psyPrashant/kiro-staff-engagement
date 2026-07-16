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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InteractionService {

	private final InteractionRepository interactionRepository;
	private final EmployeeRepository employeeRepository;
	private final UserRepository userRepository;
	private final ProjectRepository projectRepository;
	private final TaskRepository taskRepository;

	public Interaction create(CreateInteractionRequest request) {
		Employee employee = employeeRepository.findById(request.employeeId())
				.orElseThrow(() -> new IllegalArgumentException("Employee not found with id: " + request.employeeId()));

		User conductedBy = userRepository.findById(request.conductedByUserId())
				.orElseThrow(() -> new IllegalArgumentException("User not found with id: " + request.conductedByUserId()));

		User loggedBy = userRepository.findById(request.loggedByUserId())
				.orElseThrow(() -> new IllegalArgumentException("User not found with id: " + request.loggedByUserId()));

		Project project = null;
		if (request.projectId() != null) {
			project = projectRepository.findById(request.projectId())
					.orElseThrow(() -> new IllegalArgumentException("Project not found with id: " + request.projectId()));
		}

		Interaction interaction = new Interaction();
		interaction.setEmployee(employee);
		interaction.setConductedBy(conductedBy);
		interaction.setLoggedBy(loggedBy);
		interaction.setProject(project);
		interaction.setType(request.type());
		interaction.setNotes(request.notes());
		interaction.setOccurredAt(request.occurredAt());

		return interactionRepository.save(interaction);
	}

	@Transactional
	public Interaction update(Long id, UpdateInteractionRequest request) {
		Interaction interaction = interactionRepository.findById(id)
				.orElseThrow(() -> new InteractionNotFoundException(id));

		Project project = null;
		if (request.projectId() != null) {
			project = projectRepository.findById(request.projectId())
					.orElseThrow(() -> new IllegalArgumentException("Project not found with id: " + request.projectId()));
		}

		interaction.setType(request.type());
		interaction.setNotes(request.notes());
		interaction.setOccurredAt(request.occurredAt());
		interaction.setProject(project);

		return interactionRepository.save(interaction);
	}

	@Transactional
	public void delete(Long id) {
		Interaction interaction = interactionRepository.findById(id)
				.orElseThrow(() -> new InteractionNotFoundException(id));

		// Detach tasks linked to this interaction so the FK does not block deletion.
		List<Task> linkedTasks = taskRepository.findByInteractionIdIn(List.of(id));
		for (Task task : linkedTasks) {
			task.setInteraction(null);
		}
		taskRepository.saveAll(linkedTasks);

		interactionRepository.delete(interaction);
	}
}
