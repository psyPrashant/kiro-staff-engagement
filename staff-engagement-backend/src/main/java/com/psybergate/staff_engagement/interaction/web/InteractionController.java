package com.psybergate.staff_engagement.interaction.web;

import com.psybergate.staff_engagement.interaction.domain.Interaction;
import com.psybergate.staff_engagement.interaction.dto.CreateInteractionRequest;
import com.psybergate.staff_engagement.interaction.dto.UpdateInteractionRequest;
import com.psybergate.staff_engagement.interaction.service.InteractionService;
import jakarta.validation.Valid;
import java.util.List;
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

@RestController
@RequiredArgsConstructor
public class InteractionController {

	private final InteractionService interactionService;

	@GetMapping("/api/interactions")
	public List<Interaction> getAllInteractions() {
		return interactionService.listAll();
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
