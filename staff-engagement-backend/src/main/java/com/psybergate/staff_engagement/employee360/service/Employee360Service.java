package com.psybergate.staff_engagement.employee360.service;

import com.psybergate.staff_engagement.employee360.domain.Employee360NotFoundException;
import com.psybergate.staff_engagement.employee360.dto.Employee360Response;

/**
 * Assembles the read-only 360° view of an employee: profile, interaction
 * history, open tasks and the next scheduled interaction.
 */
public interface Employee360Service {

	/**
	 * Builds the 360° view for an employee.
	 *
	 * <p>Open tasks are drawn from two sources and deduplicated: tasks created
	 * directly against the employee, and tasks linked to one of the employee's
	 * interactions.
	 *
	 * @param employeeId the employee to view
	 * @return the assembled view
	 * @throws Employee360NotFoundException if no employee exists with the given id
	 */
	Employee360Response getEmployee360(Long employeeId);
}
