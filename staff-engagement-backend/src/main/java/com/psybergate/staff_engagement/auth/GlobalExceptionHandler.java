package com.psybergate.staff_engagement.auth;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidationException(
			MethodArgumentNotValidException ex) {
		Map<String, String> fieldErrors = new HashMap<>();
		ex.getBindingResult().getFieldErrors().forEach(error ->
				fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));

		Map<String, Object> body = new HashMap<>();
		body.put("error", "Validation failed");
		body.put("fieldErrors", fieldErrors);

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<Map<String, Object>> handleBadCredentialsException(
			BadCredentialsException ex) {
		Map<String, Object> body = Map.of("error", "Invalid credentials");
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
	}

	@ExceptionHandler(UsernameNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleUsernameNotFoundException(
			UsernameNotFoundException ex) {
		Map<String, Object> body = Map.of("error", "Invalid credentials");
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
	}
}
