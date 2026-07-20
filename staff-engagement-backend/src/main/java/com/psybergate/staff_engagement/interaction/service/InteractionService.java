package com.psybergate.staff_engagement.interaction.service;

import com.psybergate.staff_engagement.interaction.domain.Interaction;
import com.psybergate.staff_engagement.interaction.domain.InteractionNotFoundException;
import com.psybergate.staff_engagement.interaction.dto.CreateInteractionRequest;
import com.psybergate.staff_engagement.interaction.dto.UpdateInteractionRequest;
import java.util.List;

/**
 * Write operations for logged interactions.
 */
public interface InteractionService {

	/**
	 * Lists every logged interaction.
	 *
	 * @return all interactions
	 */
	List<Interaction> listAll();

	/**
	 * Logs a new interaction, resolving the employee, the conducting and logging
	 * users, and the optional project from the request.
	 *
	 * @param request the interaction to log
	 * @return the persisted interaction
	 * @throws IllegalArgumentException if a referenced employee, user or project does not exist
	 */
	Interaction create(CreateInteractionRequest request);

	/**
	 * Replaces the mutable fields of an existing interaction. Passing a {@code null}
	 * project id clears the project association.
	 *
	 * @param id      the interaction to update
	 * @param request the new field values
	 * @return the updated interaction
	 * @throws InteractionNotFoundException if no interaction exists with the given id
	 * @throws IllegalArgumentException     if the referenced project does not exist
	 */
	Interaction update(Long id, UpdateInteractionRequest request);

	/**
	 * Deletes an interaction, first detaching any tasks linked to it so the
	 * foreign key does not block the removal.
	 *
	 * @param id the interaction to delete
	 * @throws InteractionNotFoundException if no interaction exists with the given id
	 */
	void delete(Long id);
}
