package com.psybergate.staff_engagement.employee;

public class EmployeeNotFoundException extends RuntimeException {

	public EmployeeNotFoundException(Long id) {
		super("Employee not found with id: " + id);
	}
}
