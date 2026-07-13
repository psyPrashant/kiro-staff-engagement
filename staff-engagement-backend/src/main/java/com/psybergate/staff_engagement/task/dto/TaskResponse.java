package com.psybergate.staff_engagement.task.dto;

import com.psybergate.staff_engagement.task.Task;
import java.time.Instant;
import java.time.LocalDate;

public record TaskResponse(
	Long id,
	String title,
	String description,
	String status,
	LocalDate dueDate,
	Long assignedUserId,
	String assignedUserName,
	Long interactionId,
	Long employeeId,
	String employeeName,
	Instant createdAt
) {

	public static TaskResponse from(Task task) {
		return new TaskResponse(
			task.getId(),
			task.getTitle(),
			task.getDescription(),
			task.getStatus().name(),
			task.getDueDate(),
			task.getAssignedUser() != null ? task.getAssignedUser().getId() : null,
			task.getAssignedUser() != null ? task.getAssignedUser().getName() : null,
			task.getInteraction() != null ? task.getInteraction().getId() : null,
			task.getEmployee() != null ? task.getEmployee().getId() : null,
			task.getEmployee() != null ? task.getEmployee().getName() : null,
			task.getCreatedAt()
		);
	}
}
