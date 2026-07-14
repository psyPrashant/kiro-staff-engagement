package com.psybergate.staff_engagement.employee;

import com.psybergate.staff_engagement.scheduling.NextScheduledDto;

public record EmployeeListDto(
	Long id,
	String name,
	String email,
	String jobTitle,
	String managerName,       // null if no manager
	NextScheduledDto nextScheduled  // null if no upcoming interaction
) {}
