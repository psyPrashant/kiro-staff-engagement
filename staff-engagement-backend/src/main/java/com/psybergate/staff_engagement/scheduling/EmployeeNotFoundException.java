package com.psybergate.staff_engagement.scheduling;

public class EmployeeNotFoundException extends RuntimeException {

	public EmployeeNotFoundException(Long employeeId) {
		super("Employee not found with id: " + employeeId);
	}
}
