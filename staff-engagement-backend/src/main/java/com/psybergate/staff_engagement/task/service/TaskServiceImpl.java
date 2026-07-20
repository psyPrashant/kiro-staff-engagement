package com.psybergate.staff_engagement.task.service;

import com.psybergate.staff_engagement.employee.domain.Employee;
import com.psybergate.staff_engagement.employee.domain.EmployeeRepository;
import com.psybergate.staff_engagement.interaction.domain.Interaction;
import com.psybergate.staff_engagement.interaction.domain.InteractionRepository;
import com.psybergate.staff_engagement.task.domain.Task;
import com.psybergate.staff_engagement.task.domain.TaskNotFoundException;
import com.psybergate.staff_engagement.task.domain.TaskRepository;
import com.psybergate.staff_engagement.task.domain.TaskStatus;
import com.psybergate.staff_engagement.task.dto.CreateTaskRequest;
import com.psybergate.staff_engagement.task.dto.UpdateTaskRequest;
import com.psybergate.staff_engagement.user.domain.User;
import com.psybergate.staff_engagement.user.domain.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

	private final TaskRepository taskRepository;
	private final InteractionRepository interactionRepository;
	private final UserRepository userRepository;
	private final EmployeeRepository employeeRepository;

	@Override
	@Transactional(readOnly = true)
	public List<Task> listAll() {
		return taskRepository.findAll();
	}

	@Override
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

	@Override
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

	@Override
	public void delete(Long id) {
		Task task = taskRepository.findById(id)
			.orElseThrow(() -> new TaskNotFoundException(id));
		taskRepository.delete(task);
	}
}
