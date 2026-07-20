package com.psybergate.staff_engagement.scheduling.service;

import com.psybergate.staff_engagement.employee.domain.EmployeeNotFoundException;
import com.psybergate.staff_engagement.scheduling.domain.CompletionStatus;
import com.psybergate.staff_engagement.scheduling.domain.ScheduledInteractionNotFoundException;
import com.psybergate.staff_engagement.scheduling.dto.CreateScheduledInteractionRequest;
import com.psybergate.staff_engagement.scheduling.dto.ScheduledInteractionResponse;
import com.psybergate.staff_engagement.scheduling.dto.UpdateScheduledInteractionRequest;
import java.time.LocalDate;
import java.util.List;

/**
 * Scheduling of future interactions. Every operation is scoped to the user who
 * created the scheduled interaction — a user can only see and modify their own.
 */
public interface SchedulingService {

	/**
	 * Schedules an interaction in {@code PENDING} status.
	 *
	 * @param request the interaction to schedule
	 * @param userId  the user scheduling it
	 * @return the created scheduled interaction
	 * @throws IllegalArgumentException  if the scheduled date is in the past, or the user does not exist
	 * @throws EmployeeNotFoundException if the referenced employee does not exist
	 */
	ScheduledInteractionResponse create(CreateScheduledInteractionRequest request, Long userId);

	/**
	 * Lists a user's scheduled interactions in ascending date order, capped at 200 entries.
	 *
	 * @param userId     the owning user
	 * @param status     keep only this completion status; {@code null} keeps all
	 * @param employeeId keep only interactions for this employee; {@code null} keeps all
	 * @param overdue    when {@code TRUE}, keep only pending interactions dated before today
	 * @return the matching scheduled interactions
	 */
	List<ScheduledInteractionResponse> list(
			Long userId, CompletionStatus status, Long employeeId, Boolean overdue);

	/**
	 * Updates a scheduled interaction the user owns. Only {@code PENDING}
	 * interactions may be modified, and the only permitted status transitions are
	 * {@code PENDING → COMPLETED} and {@code PENDING → CANCELLED}. {@code null}
	 * request fields are left unchanged.
	 *
	 * @param id      the scheduled interaction to update
	 * @param request the new field values
	 * @param userId  the owning user
	 * @return the updated scheduled interaction
	 * @throws ScheduledInteractionNotFoundException if it does not exist or belongs to another user
	 * @throws IllegalArgumentException              if the new scheduled date is in the past
	 * @throws IllegalStateException                 if the status transition is not permitted
	 */
	ScheduledInteractionResponse update(
			Long id, UpdateScheduledInteractionRequest request, Long userId);

	/**
	 * Deletes a scheduled interaction the user owns.
	 *
	 * @param id     the scheduled interaction to delete
	 * @param userId the owning user
	 * @throws ScheduledInteractionNotFoundException if it does not exist or belongs to another user
	 */
	void delete(Long id, Long userId);

	/**
	 * Counts the user's pending scheduled interactions dated before today.
	 *
	 * @param userId the owning user
	 * @return the overdue count
	 */
	long countOverdue(Long userId);

	/**
	 * Reports whether a scheduled interaction is overdue as of today.
	 *
	 * @param scheduledDate the scheduled date
	 * @param status        the completion status
	 * @return {@code true} when still pending and dated before today
	 */
	boolean isOverdue(LocalDate scheduledDate, CompletionStatus status);
}
