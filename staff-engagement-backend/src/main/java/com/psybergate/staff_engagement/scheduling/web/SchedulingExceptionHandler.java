package com.psybergate.staff_engagement.scheduling.web;

import com.psybergate.staff_engagement.employee.domain.EmployeeNotFoundException;
import com.psybergate.staff_engagement.scheduling.domain.ScheduledInteractionNotFoundException;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = SchedulingController.class)
@Order(1)
public class SchedulingExceptionHandler {

	@ExceptionHandler(EmployeeNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public Map<String, String> handleEmployeeNotFound(EmployeeNotFoundException ex) {
		return Map.of("message", ex.getMessage());
	}

	@ExceptionHandler(ScheduledInteractionNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public Map<String, String> handleScheduledInteractionNotFound(ScheduledInteractionNotFoundException ex) {
		return Map.of("message", ex.getMessage());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Map<String, String> handleIllegalArgument(IllegalArgumentException ex) {
		return Map.of("message", ex.getMessage());
	}

	@ExceptionHandler(IllegalStateException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Map<String, String> handleIllegalState(IllegalStateException ex) {
		return Map.of("message", ex.getMessage());
	}
}
