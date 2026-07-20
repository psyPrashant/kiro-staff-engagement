package com.psybergate.staff_engagement.employee.web;

import com.psybergate.staff_engagement.employee.domain.Employee;
import com.psybergate.staff_engagement.employee.dto.CreateEmployeeRequest;
import com.psybergate.staff_engagement.employee.dto.EmployeeListDto;
import com.psybergate.staff_engagement.employee.service.EmployeeService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class EmployeeController {

	private final EmployeeService employeeService;

	@GetMapping("/api/employees")
	public List<EmployeeListDto> getAllEmployees() {
		return employeeService.listAll();
	}

	@PostMapping("/api/employees")
	public ResponseEntity<EmployeeListDto> createEmployee(@RequestBody @Valid CreateEmployeeRequest request) {
		Employee saved = employeeService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(EmployeeListDto.from(saved, null));
	}

	@DeleteMapping("/api/employees/{id}")
	public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
		employeeService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
