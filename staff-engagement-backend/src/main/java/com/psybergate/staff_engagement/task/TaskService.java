package com.psybergate.staff_engagement.task;

import com.psybergate.staff_engagement.interaction.Interaction;
import com.psybergate.staff_engagement.interaction.InteractionRepository;
import com.psybergate.staff_engagement.task.dto.CreateTaskRequest;
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

		Task task = new Task();
		task.setTitle(request.title());
		task.setDescription(request.description());
		task.setInteraction(interaction);
		task.setDueDate(request.dueDate());
		task.setAssignedUser(assignedUser);
		task.setStatus(TaskStatus.OPEN);

		return taskRepository.save(task);
	}
}
