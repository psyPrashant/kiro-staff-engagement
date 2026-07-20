package com.psybergate.staff_engagement.client.dto;

/** Request to update a project's name and/or owning company. */
public record UpdateProjectRequest(
		String name,
		Long companyId) {
}
