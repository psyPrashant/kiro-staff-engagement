package com.psybergate.staff_engagement.scheduling.service;

import com.psybergate.staff_engagement.scheduling.dto.NextScheduledDto;
import java.util.List;
import java.util.Map;

/**
 * Look-up of the next pending scheduled interaction per employee.
 */
public interface NextScheduledInteractionService {

	/**
	 * Finds the earliest pending scheduled interaction for an employee that falls
	 * on or after today.
	 *
	 * @param employeeId the employee to look up
	 * @return the next scheduled interaction, or {@code null} if there is none
	 * @throws IllegalArgumentException if {@code employeeId} is {@code null}
	 */
	NextScheduledDto getNextScheduled(Long employeeId);

	/**
	 * Batch equivalent of {@link #getNextScheduled(Long)}. Employees with no
	 * pending scheduled interaction are absent from the result.
	 *
	 * @param employeeIds the employees to look up; at most 200
	 * @return employee id to next scheduled interaction
	 * @throws IllegalArgumentException if {@code employeeIds} is {@code null} or holds more than 200 ids
	 */
	Map<Long, NextScheduledDto> getNextScheduledBatch(List<Long> employeeIds);
}
