package com.psybergate.staff_engagement.interaction;

import com.psybergate.staff_engagement.client.Project;
import com.psybergate.staff_engagement.client.ProjectRepository;
import com.psybergate.staff_engagement.employee.Employee;
import com.psybergate.staff_engagement.employee.EmployeeRepository;
import com.psybergate.staff_engagement.interaction.dto.CreateInteractionRequest;
import com.psybergate.staff_engagement.user.User;
import com.psybergate.staff_engagement.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InteractionService {

	private final InteractionRepository interactionRepository;
	private final EmployeeRepository employeeRepository;
	private final UserRepository userRepository;
	private final ProjectRepository projectRepository;

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
}
