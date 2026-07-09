package com.psybergate.staff_engagement.employee;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class EmployeeController {

	private final EmployeeRepository employeeRepository;

	@GetMapping("/api/employees")
	public List<Employee> getAllEmployees() {
		return employeeRepository.findAll();
	}
}
