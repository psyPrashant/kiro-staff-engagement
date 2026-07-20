package com.psybergate.staff_engagement.task.service;

import com.psybergate.staff_engagement.task.domain.Task;
import com.psybergate.staff_engagement.task.domain.TaskNotFoundException;
import com.psybergate.staff_engagement.task.dto.CreateTaskRequest;
import com.psybergate.staff_engagement.task.dto.UpdateTaskRequest;
import java.util.List;

/**
 * Write operations for tasks.
 */
public interface TaskService {

	/**
	 * Lists every task.
	 *
	 * @return all tasks
	 */
	List<Task> listAll();

	/**
	 * Creates a task in {@code OPEN} status, resolving the optional interaction,
	 * assigned user and employee references from the request.
	 *
	 * @param request the task to create
	 * @return the persisted task
	 * @throws IllegalArgumentException if a referenced interaction, user or employee does not exist
	 */
	Task create(CreateTaskRequest request);

	/**
	 * Replaces the mutable fields of an existing task. A {@code null} status on the
	 * request leaves the current status untouched.
	 *
	 * @param id      the task to update
	 * @param request the new field values
	 * @return the updated task
	 * @throws TaskNotFoundException    if no task exists with the given id
	 * @throws IllegalArgumentException if a referenced interaction, user or employee does not exist
	 */
	Task update(Long id, UpdateTaskRequest request);

	/**
	 * Deletes a task.
	 *
	 * @param id the task to delete
	 * @throws TaskNotFoundException if no task exists with the given id
	 */
	void delete(Long id);
}
