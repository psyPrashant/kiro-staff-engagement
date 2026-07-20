package com.psybergate.staff_engagement.employee.service;

import com.psybergate.staff_engagement.employee.domain.Employee;
import com.psybergate.staff_engagement.employee.domain.EmployeeNotFoundException;
import com.psybergate.staff_engagement.employee.domain.EmployeeRepository;
import com.psybergate.staff_engagement.employee.dto.CreateEmployeeRequest;
import com.psybergate.staff_engagement.employee.dto.EmployeeListDto;
import com.psybergate.staff_engagement.interaction.domain.Interaction;
import com.psybergate.staff_engagement.interaction.domain.InteractionRepository;
import com.psybergate.staff_engagement.scheduling.domain.ScheduledInteractionRepository;
import com.psybergate.staff_engagement.scheduling.dto.NextScheduledDto;
import com.psybergate.staff_engagement.scheduling.service.NextScheduledInteractionService;
import com.psybergate.staff_engagement.task.domain.Task;
import com.psybergate.staff_engagement.task.domain.TaskRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

	private final EmployeeRepository employeeRepository;
	private final InteractionRepository interactionRepository;
	private final TaskRepository taskRepository;
	private final ScheduledInteractionRepository scheduledInteractionRepository;
	private final NextScheduledInteractionService nextScheduledInteractionService;

	@Transactional(readOnly = true)
	@Override
	public List<EmployeeListDto> listAll() {
		List<Employee> employees = employeeRepository.findAll();

		List<Long> ids = employees.stream().map(Employee::getId).toList();
		Map<Long, NextScheduledDto> nextByEmployeeId =
				nextScheduledInteractionService.getNextScheduledBatch(ids);

		return employees.stream()
				.map(employee -> EmployeeListDto.from(employee, nextByEmployeeId.get(employee.getId())))
				.toList();
	}

	@Transactional(readOnly = true)
	@Override
	public boolean existsById(Long id) {
		return employeeRepository.existsById(id);
	}

	@Transactional
	@Override
	public Employee create(CreateEmployeeRequest request) {
		Employee employee = new Employee();
		employee.setName(request.name().trim());
		employee.setEmail(request.email().trim());
		employee.setJobTitle(request.jobTitle() != null ? request.jobTitle().trim() : null);

		if (request.managerId() != null) {
			Employee manager = employeeRepository.findById(request.managerId())
					.orElseThrow(() -> new IllegalArgumentException(
							"Manager not found with id: " + request.managerId()));
			employee.setManager(manager);
		}

		return employeeRepository.save(employee);
	}

	/**
	 * Deletes an employee together with the records that reference it, so the
	 * removal does not violate foreign-key constraints. Tasks linked either
	 * directly to the employee or to one of the employee's interactions are
	 * removed, along with the employee's interactions and scheduled
	 * interactions. Employees who reported to the deleted employee have their
	 * manager reference cleared.
	 */
	@Transactional
	@Override
	public void delete(Long id) {
		Employee employee = employeeRepository.findById(id)
				.orElseThrow(() -> new EmployeeNotFoundException(id));

		List<Interaction> interactions = interactionRepository.findByEmployeeIdOrderByOccurredAtDesc(id);
		List<Long> interactionIds = interactions.stream().map(Interaction::getId).toList();

		Set<Task> tasksToDelete = new LinkedHashSet<>(taskRepository.findByEmployeeId(id));
		if (!interactionIds.isEmpty()) {
			tasksToDelete.addAll(taskRepository.findByInteractionIdIn(interactionIds));
		}
		taskRepository.deleteAll(tasksToDelete);

		scheduledInteractionRepository.deleteAll(scheduledInteractionRepository.findByEmployeeId(id));
		interactionRepository.deleteAll(interactions);

		List<Employee> reports = employeeRepository.findByManagerId(id);
		for (Employee report : reports) {
			report.setManager(null);
		}
		employeeRepository.saveAll(reports);

		employeeRepository.delete(employee);
	}
}
