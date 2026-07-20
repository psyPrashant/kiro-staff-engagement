package com.psybergate.staff_engagement.employee360.web;

import com.psybergate.staff_engagement.employee360.dto.Employee360Response;
import com.psybergate.staff_engagement.employee360.service.Employee360Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class Employee360Controller {

	private final Employee360Service employee360Service;

	@GetMapping("/api/employees/{id}/360")
	public ResponseEntity<Employee360Response> getEmployee360(
			@PathVariable("id") Long id) {
		return ResponseEntity.ok(employee360Service.getEmployee360(id));
	}
}
