package com.psybergate.staff_engagement.task;

import com.psybergate.staff_engagement.task.dto.CreateTaskRequest;
import com.psybergate.staff_engagement.task.dto.TaskResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TaskController {

	private final TaskRepository taskRepository;
	private final TaskService taskService;

	@GetMapping("/api/tasks")
	public List<TaskResponse> getAllTasks() {
		return taskRepository.findAll().stream()
			.map(TaskResponse::from)
			.toList();
	}

	@PostMapping("/api/tasks")
	public ResponseEntity<TaskResponse> createTask(@RequestBody @Valid CreateTaskRequest request) {
		Task savedTask = taskService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(TaskResponse.from(savedTask));
	}
}
