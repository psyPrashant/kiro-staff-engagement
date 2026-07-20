package com.psybergate.staff_engagement.client.web;

import com.psybergate.staff_engagement.client.domain.Project;
import com.psybergate.staff_engagement.client.dto.CreateProjectRequest;
import com.psybergate.staff_engagement.client.dto.ProjectDetailDto;
import com.psybergate.staff_engagement.client.dto.ProjectSummaryDto;
import com.psybergate.staff_engagement.client.dto.UpdateProjectRequest;
import com.psybergate.staff_engagement.client.service.ClientService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProjectController {

	private final ClientService clientService;

	@GetMapping("/api/projects")
	public List<Project> getProjects(@RequestParam(required = false) Long companyId) {
		return clientService.listProjects(companyId);
	}

	@GetMapping("/api/projects/summaries")
	public List<ProjectSummaryDto> getProjectSummaries() {
		return clientService.listProjectSummaries();
	}

	@GetMapping("/api/projects/{id}")
	public ProjectDetailDto getProject(@PathVariable Long id) {
		return clientService.getProjectDetail(id);
	}

	@PostMapping("/api/projects")
	@ResponseStatus(HttpStatus.CREATED)
	public ProjectDetailDto createProject(@RequestBody CreateProjectRequest request) {
		return clientService.createProject(request);
	}

	@PutMapping("/api/projects/{id}")
	public ProjectDetailDto updateProject(@PathVariable Long id, @RequestBody UpdateProjectRequest request) {
		return clientService.updateProject(id, request);
	}
}
