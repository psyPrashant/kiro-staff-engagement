package com.psybergate.staff_engagement.interaction.service;

import com.psybergate.staff_engagement.client.domain.Project;
import com.psybergate.staff_engagement.client.domain.ProjectRepository;
import com.psybergate.staff_engagement.employee.domain.Employee;
import com.psybergate.staff_engagement.employee.domain.EmployeeRepository;
import com.psybergate.staff_engagement.interaction.domain.Interaction;
import com.psybergate.staff_engagement.interaction.domain.InteractionNotFoundException;
import com.psybergate.staff_engagement.interaction.domain.InteractionRepository;
import com.psybergate.staff_engagement.interaction.dto.CreateInteractionRequest;
import com.psybergate.staff_engagement.interaction.dto.UpdateInteractionRequest;
import com.psybergate.staff_engagement.task.domain.Task;
import com.psybergate.staff_engagement.task.domain.TaskRepository;
import com.psybergate.staff_engagement.user.domain.User;
import com.psybergate.staff_engagement.user.domain.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InteractionServiceImpl implements InteractionService {

	private final InteractionRepository interactionRepository;
	private final EmployeeRepository employeeRepository;
	private final UserRepository userRepository;
	private final ProjectRepository projectRepository;
	private final TaskRepository taskRepository;

	@Override
	@Transactional(readOnly = true)
	public List<Interaction> listAll() {
		return interactionRepository.findAll();
	}

	@Override
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
	@Override
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
	@Override
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
