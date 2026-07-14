package com.psybergate.staff_engagement.employee360;

import com.psybergate.staff_engagement.employee.Employee;
import com.psybergate.staff_engagement.employee.EmployeeRepository;
import com.psybergate.staff_engagement.interaction.InteractionRepository;
import com.psybergate.staff_engagement.scheduling.NextScheduledInteractionService;
import com.psybergate.staff_engagement.task.TaskRepository;
import net.jqwik.api.*;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Property-based tests for Employee 360 profile mapping.
 *
 * Validates: Requirements 1.1
 */
class Employee360ProfilePropertyTest {

	private final EmployeeRepository employeeRepository = Mockito.mock(EmployeeRepository.class);
	private final InteractionRepository interactionRepository = Mockito.mock(InteractionRepository.class);
	private final TaskRepository taskRepository = Mockito.mock(TaskRepository.class);
	private final NextScheduledInteractionService nextScheduledInteractionService = Mockito.mock(NextScheduledInteractionService.class);
	private final Employee360Service employee360Service = new Employee360Service(
		employeeRepository, interactionRepository, taskRepository, nextScheduledInteractionService
	);

	/**
	 * Property 1: Profile mapping preserves all employee fields
	 *
	 * For any Employee entity with non-null required fields, the assembled ProfileDto
	 * SHALL contain the same id, name, email, jobTitle, and the manager's name
	 * (or null if no manager exists).
	 *
	 * Validates: Requirements 1.1
	 */
	@Property(tries = 100)
	@Tag("Feature: employee-360-view, Property 1: Profile mapping preserves all employee fields")
	void profileMappingPreservesAllEmployeeFields(@ForAll("employees") Employee employee) {
		// Arrange: mock repository to return the generated employee
		when(employeeRepository.findById(eq(employee.getId()))).thenReturn(Optional.of(employee));
		when(interactionRepository.findByEmployeeIdOrderByOccurredAtDesc(eq(employee.getId())))
			.thenReturn(List.of());

		// Act
		Employee360Response response = employee360Service.getEmployee360(employee.getId());

		// Assert: profile fields match the source entity
		ProfileDto profile = response.profile();
		assertThat(profile.id()).isEqualTo(employee.getId());
		assertThat(profile.name()).isEqualTo(employee.getName());
		assertThat(profile.email()).isEqualTo(employee.getEmail());
		assertThat(profile.jobTitle()).isEqualTo(employee.getJobTitle());

		if (employee.getManager() != null) {
			assertThat(profile.managerName()).isEqualTo(employee.getManager().getName());
		} else {
			assertThat(profile.managerName()).isNull();
		}
	}

	/**
	 * Generates arbitrary Employee entities with random fields and optional manager.
	 */
	@Provide
	Arbitrary<Employee> employees() {
		Arbitrary<Long> ids = Arbitraries.longs().between(1L, 100_000L);
		Arbitrary<String> names = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50);
		Arbitrary<String> emails = Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20)
			.map(s -> s + "@example.com");
		Arbitrary<String> jobTitles = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(30);
		Arbitrary<Optional<Employee>> managers = Arbitraries.oneOf(
			Arbitraries.just(Optional.empty()),
			buildManagerArbitrary().map(Optional::of)
		);

		return Combinators.combine(ids, names, emails, jobTitles, managers)
			.as((id, name, email, jobTitle, manager) -> {
				Employee emp = new Employee();
				emp.setId(id);
				emp.setName(name);
				emp.setEmail(email);
				emp.setJobTitle(jobTitle);
				emp.setCreatedAt(Instant.now());
				manager.ifPresent(emp::setManager);
				return emp;
			});
	}

	private Arbitrary<Employee> buildManagerArbitrary() {
		Arbitrary<Long> ids = Arbitraries.longs().between(100_001L, 200_000L);
		Arbitrary<String> names = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50);
		Arbitrary<String> emails = Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20)
			.map(s -> s + "@manager.com");
		Arbitrary<String> jobTitles = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(30);

		return Combinators.combine(ids, names, emails, jobTitles)
			.as((id, name, email, jobTitle) -> {
				Employee mgr = new Employee();
				mgr.setId(id);
				mgr.setName(name);
				mgr.setEmail(email);
				mgr.setJobTitle(jobTitle);
				mgr.setCreatedAt(Instant.now());
				return mgr;
			});
	}
}
