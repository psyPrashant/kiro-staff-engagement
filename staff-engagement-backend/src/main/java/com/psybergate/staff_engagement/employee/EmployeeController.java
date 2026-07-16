package com.psybergate.staff_engagement.employee;

import com.psybergate.staff_engagement.scheduling.NextScheduledDto;
import com.psybergate.staff_engagement.scheduling.NextScheduledInteractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class EmployeeController {

	private final EmployeeRepository employeeRepository;
	private final EmployeeService employeeService;
	private final NextScheduledInteractionService nextScheduledService;

	@GetMapping("/api/employees")
	public List<EmployeeListDto> getAllEmployees() {
		List<Employee> employees = employeeRepository.findAll();

		List<Long> ids = employees.stream().map(Employee::getId).toList();
		Map<Long, NextScheduledDto> nextMap = nextScheduledService.getNextScheduledBatch(ids);

		return employees.stream()
				.map(e -> new EmployeeListDto(
						e.getId(),
						e.getName(),
						e.getEmail(),
						e.getJobTitle(),
						e.getManager() != null ? e.getManager().getName() : null,
						nextMap.get(e.getId())
				))
				.toList();
	}

	@PostMapping("/api/employees")
	public ResponseEntity<EmployeeListDto> createEmployee(@RequestBody @Valid CreateEmployeeRequest request) {
		Employee saved = employeeService.create(request);
		EmployeeListDto dto = new EmployeeListDto(
				saved.getId(),
				saved.getName(),
				saved.getEmail(),
				saved.getJobTitle(),
				saved.getManager() != null ? saved.getManager().getName() : null,
				null
		);
		return ResponseEntity.status(HttpStatus.CREATED).body(dto);
	}

	@DeleteMapping("/api/employees/{id}")
	public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
		employeeService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
