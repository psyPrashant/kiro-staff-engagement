package com.psybergate.staff_engagement.client.dto;

import java.util.List;

/** Detail model for a single project, including the employees assigned to it. */
public record ProjectDetailDto(
		Long id,
		String name,
		Long companyId,
		String companyName,
		List<AssignedEmployeeDto> employees) {
}
