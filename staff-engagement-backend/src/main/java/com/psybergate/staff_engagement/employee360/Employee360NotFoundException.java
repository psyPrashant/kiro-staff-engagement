package com.psybergate.staff_engagement.employee360;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class Employee360NotFoundException extends RuntimeException {

	public Employee360NotFoundException(Long id) {
		super("Employee not found with id: " + id);
	}
}
