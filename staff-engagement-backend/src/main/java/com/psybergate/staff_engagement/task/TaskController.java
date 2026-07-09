package com.psybergate.staff_engagement.task;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TaskController {

	private final TaskRepository taskRepository;

	@GetMapping("/api/tasks")
	public List<Task> getAllTasks() {
		return taskRepository.findAll();
	}
}
