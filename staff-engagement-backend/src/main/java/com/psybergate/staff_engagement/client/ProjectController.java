package com.psybergate.staff_engagement.client;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProjectController {

	private final ProjectRepository projectRepository;

	@GetMapping("/api/projects")
	public List<Project> getProjects() {
		return projectRepository.findAll();
	}
}
