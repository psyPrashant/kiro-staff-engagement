package com.psybergate.staff_engagement.task;

import com.psybergate.staff_engagement.employee.Employee;
import com.psybergate.staff_engagement.employee.EmployeeRepository;
import com.psybergate.staff_engagement.interaction.Interaction;
import com.psybergate.staff_engagement.interaction.InteractionRepository;
import com.psybergate.staff_engagement.task.dto.CreateTaskRequest;
import com.psybergate.staff_engagement.task.dto.UpdateTaskRequest;
import com.psybergate.staff_engagement.user.User;
import com.psybergate.staff_engagement.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaskService {

	private final TaskRepository taskRepository;
	private final InteractionRepository interactionRepository;
	private final UserRepository userRepository;
	private final EmployeeRepository employeeRepository;

	public Task create(CreateTaskRequest request) {
		Interaction interaction = null;
		if (request.interactionId() != null) {
			interaction = interactionRepository.findById(request.interactionId())
				.orElseThrow(() -> new IllegalArgumentException("Interaction not found with id: " + request.interactionId()));
		}

		User assignedUser = null;
		if (request.assignedUserId() != null) {
			assignedUser = userRepository.findById(request.assignedUserId())
				.orElseThrow(() -> new IllegalArgumentException("User not found with id: " + request.assignedUserId()));
		}

		Employee employee = null;
		if (request.employeeId() != null) {
			employee = employeeRepository.findById(request.employeeId())
				.orElseThrow(() -> new IllegalArgumentException("Employee not found with id: " + request.employeeId()));
		}

		Task task = new Task();
		task.setTitle(request.title());
		task.setDescription(request.description());
		task.setInteraction(interaction);
		task.setDueDate(request.dueDate());
		task.setAssignedUser(assignedUser);
		task.setEmployee(employee);
		task.setStatus(TaskStatus.OPEN);

		return taskRepository.save(task);
	}

	public Task update(Long id, UpdateTaskRequest request) {
		Task task = taskRepository.findById(id)
			.orElseThrow(() -> new TaskNotFoundException(id));

		Interaction interaction = null;
		if (request.interactionId() != null) {
			interaction = interactionRepository.findById(request.interactionId())
				.orElseThrow(() -> new IllegalArgumentException("Interaction not found with id: " + request.interactionId()));
		}

		User assignedUser = null;
		if (request.assignedUserId() != null) {
			assignedUser = userRepository.findById(request.assignedUserId())
				.orElseThrow(() -> new IllegalArgumentException("User not found with id: " + request.assignedUserId()));
		}

		Employee employee = null;
		if (request.employeeId() != null) {
			employee = employeeRepository.findById(request.employeeId())
				.orElseThrow(() -> new IllegalArgumentException("Employee not found with id: " + request.employeeId()));
		}

		task.setTitle(request.title());
		task.setDescription(request.description());
		task.setDueDate(request.dueDate());
		task.setInteraction(interaction);
		task.setAssignedUser(assignedUser);
		task.setEmployee(employee);
		task.setStatus(request.status() != null ? request.status() : task.getStatus());

		return taskRepository.save(task);
	}

	public void delete(Long id) {
		Task task = taskRepository.findById(id)
			.orElseThrow(() -> new TaskNotFoundException(id));
		taskRepository.delete(task);
	}
}
