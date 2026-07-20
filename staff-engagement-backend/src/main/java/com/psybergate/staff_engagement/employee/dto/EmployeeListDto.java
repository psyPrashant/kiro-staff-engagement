package com.psybergate.staff_engagement.employee.dto;

import com.psybergate.staff_engagement.employee.domain.Employee;
import com.psybergate.staff_engagement.scheduling.dto.NextScheduledDto;

public record EmployeeListDto(
	Long id,
	String name,
	String email,
	String jobTitle,
	String managerName,       // null if no manager
	NextScheduledDto nextScheduled  // null if no upcoming interaction
) {

	/**
	 * Maps an employee onto its list representation.
	 *
	 * @param employee      the employee to map
	 * @param nextScheduled the employee's next scheduled interaction, or null if none
	 * @return the mapped DTO
	 */
	public static EmployeeListDto from(Employee employee, NextScheduledDto nextScheduled) {
		return new EmployeeListDto(
			employee.getId(),
			employee.getName(),
			employee.getEmail(),
			employee.getJobTitle(),
			employee.getManager() != null ? employee.getManager().getName() : null,
			nextScheduled
		);
	}
}
