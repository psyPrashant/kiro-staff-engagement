package com.psybergate.staff_engagement.employee;

import com.psybergate.staff_engagement.scheduling.NextScheduledDto;
import com.psybergate.staff_engagement.scheduling.NextScheduledInteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class EmployeeController {

	private final EmployeeRepository employeeRepository;
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
}
