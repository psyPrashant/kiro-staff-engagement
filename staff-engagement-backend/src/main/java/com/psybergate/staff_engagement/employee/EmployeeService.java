package com.psybergate.staff_engagement.employee;

import com.psybergate.staff_engagement.interaction.Interaction;
import com.psybergate.staff_engagement.interaction.InteractionRepository;
import com.psybergate.staff_engagement.scheduling.ScheduledInteractionRepository;
import com.psybergate.staff_engagement.task.Task;
import com.psybergate.staff_engagement.task.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EmployeeService {

	private final EmployeeRepository employeeRepository;
	private final InteractionRepository interactionRepository;
	private final TaskRepository taskRepository;
	private final ScheduledInteractionRepository scheduledInteractionRepository;

	@Transactional
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
