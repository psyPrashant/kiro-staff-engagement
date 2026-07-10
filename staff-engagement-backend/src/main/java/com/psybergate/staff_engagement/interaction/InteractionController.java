package com.psybergate.staff_engagement.interaction;

import com.psybergate.staff_engagement.interaction.dto.CreateInteractionRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class InteractionController {

	private final InteractionRepository interactionRepository;
	private final InteractionService interactionService;

	@GetMapping("/api/interactions")
	public List<Interaction> getAllInteractions() {
		return interactionRepository.findAll();
	}

	@PostMapping("/api/interactions")
	public ResponseEntity<Interaction> createInteraction(@RequestBody @Valid CreateInteractionRequest request) {
		Interaction savedInteraction = interactionService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(savedInteraction);
	}
}
