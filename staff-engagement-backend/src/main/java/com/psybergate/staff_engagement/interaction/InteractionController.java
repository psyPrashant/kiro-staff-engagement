package com.psybergate.staff_engagement.interaction;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class InteractionController {

	private final InteractionRepository interactionRepository;

	@GetMapping("/api/interactions")
	public List<Interaction> getAllInteractions() {
		return interactionRepository.findAll();
	}
}
