package com.psybergate.staff_engagement.interaction;

public class InteractionNotFoundException extends RuntimeException {

	public InteractionNotFoundException(Long id) {
		super("Interaction not found with id: " + id);
	}
}
