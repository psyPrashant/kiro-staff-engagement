package com.psybergate.staff_engagement.task.dto;

import com.psybergate.staff_engagement.task.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateTaskRequest(
	@NotBlank @Size(max = 255) String title,
	@Size(max = 2000) String description,
	Long interactionId,
	Long employeeId,
	LocalDate dueDate,
	Long assignedUserId,
	TaskStatus status
) {}
