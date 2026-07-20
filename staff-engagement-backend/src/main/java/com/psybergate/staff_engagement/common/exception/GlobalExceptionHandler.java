package com.psybergate.staff_engagement.common.exception;

import com.psybergate.staff_engagement.employee.domain.EmployeeNotFoundException;
import com.psybergate.staff_engagement.interaction.domain.InteractionNotFoundException;
import com.psybergate.staff_engagement.task.domain.TaskNotFoundException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
		Map<String, String> fieldErrors = new HashMap<>();
		ex.getBindingResult().getFieldErrors().forEach(fe ->
			fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
		return new ErrorResponse("Validation failed", fieldErrors);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleIllegalArgument(IllegalArgumentException ex) {
		return new ErrorResponse(ex.getMessage(), null);
	}

	@ExceptionHandler(TaskNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public ErrorResponse handleTaskNotFound(TaskNotFoundException ex) {
		return new ErrorResponse(ex.getMessage(), null);
	}

	@ExceptionHandler(EmployeeNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public ErrorResponse handleEmployeeNotFound(EmployeeNotFoundException ex) {
		return new ErrorResponse(ex.getMessage(), null);
	}

	@ExceptionHandler(InteractionNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public ErrorResponse handleInteractionNotFound(InteractionNotFoundException ex) {
		return new ErrorResponse(ex.getMessage(), null);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleMessageNotReadable(HttpMessageNotReadableException ex) {
		return new ErrorResponse("Malformed request body", null);
	}

	@ExceptionHandler(BadCredentialsException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public ErrorResponse handleBadCredentials(BadCredentialsException ex) {
		return new ErrorResponse("Invalid credentials", null);
	}

	@ExceptionHandler(UsernameNotFoundException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public ErrorResponse handleUsernameNotFound(UsernameNotFoundException ex) {
		return new ErrorResponse("Invalid credentials", null);
	}

	@ExceptionHandler(DataAccessException.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public ErrorResponse handleDataAccessException(DataAccessException ex) {
		return new ErrorResponse("Unable to compute engagement matrix due to a data access failure", null);
	}
}
