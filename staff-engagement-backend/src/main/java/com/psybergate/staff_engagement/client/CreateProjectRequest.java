package com.psybergate.staff_engagement.client;

/**
 * Request to create a project. Either {@code companyId} (existing company) or
 * {@code newCompanyName} (create a company inline) must be provided.
 */
public record CreateProjectRequest(
		String name,
		Long companyId,
		String newCompanyName) {
}
