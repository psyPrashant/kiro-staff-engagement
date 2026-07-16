package com.psybergate.staff_engagement.client;

/**
 * Row model for the Companies/Projects list view.
 * {@code employeeCount} is the number of distinct employees that have an
 * interaction against the project (there is no direct employee-project link).
 */
public record ProjectSummaryDto(
		Long id,
		String name,
		Long companyId,
		String companyName,
		long employeeCount) {
}
