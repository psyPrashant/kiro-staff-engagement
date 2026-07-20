package com.psybergate.staff_engagement.task.web;

import com.psybergate.staff_engagement.task.domain.Task;
import com.psybergate.staff_engagement.task.dto.CreateTaskRequest;
import com.psybergate.staff_engagement.task.dto.TaskResponse;
import com.psybergate.staff_engagement.task.dto.UpdateTaskRequest;
import com.psybergate.staff_engagement.task.service.TaskService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TaskController {

	private final TaskService taskService;

	@GetMapping("/api/tasks")
	public List<TaskResponse> getAllTasks() {
		return taskService.listAll().stream()
			.map(TaskResponse::from)
			.toList();
	}

	@PostMapping("/api/tasks")
	public ResponseEntity<TaskResponse> createTask(@RequestBody @Valid CreateTaskRequest request) {
		Task savedTask = taskService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(TaskResponse.from(savedTask));
	}

	@PutMapping("/api/tasks/{id}")
	public ResponseEntity<TaskResponse> updateTask(@PathVariable Long id, @RequestBody @Valid UpdateTaskRequest request) {
		Task updatedTask = taskService.update(id, request);
		return ResponseEntity.ok(TaskResponse.from(updatedTask));
	}

	@DeleteMapping("/api/tasks/{id}")
	public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
		taskService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
