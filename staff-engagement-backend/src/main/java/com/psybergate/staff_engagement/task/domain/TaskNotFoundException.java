package com.psybergate.staff_engagement.task.domain;

public class TaskNotFoundException extends RuntimeException {
	public TaskNotFoundException(Long id) {
		super("Task not found with id: " + id);
	}
}
