package com.psybergate.staff_engagement.interaction;

import com.psybergate.staff_engagement.interaction.dto.CreateInteractionRequest;
import com.psybergate.staff_engagement.interaction.dto.UpdateInteractionRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

	@PutMapping("/api/interactions/{id}")
	public ResponseEntity<Interaction> updateInteraction(
			@PathVariable Long id,
			@RequestBody @Valid UpdateInteractionRequest request) {
		Interaction updated = interactionService.update(id, request);
		return ResponseEntity.ok(updated);
	}

	@DeleteMapping("/api/interactions/{id}")
	public ResponseEntity<Void> deleteInteraction(@PathVariable Long id) {
		interactionService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
