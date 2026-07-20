package com.psybergate.staff_engagement.employee360.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.psybergate.staff_engagement.employee.domain.Employee;
import com.psybergate.staff_engagement.employee.domain.EmployeeRepository;
import com.psybergate.staff_engagement.employee360.dto.Employee360Response;
import com.psybergate.staff_engagement.employee360.dto.InteractionDto;
import com.psybergate.staff_engagement.employee360.service.Employee360Service;
import com.psybergate.staff_engagement.employee360.service.Employee360ServiceImpl;
import com.psybergate.staff_engagement.interaction.domain.Interaction;
import com.psybergate.staff_engagement.interaction.domain.InteractionRepository;
import com.psybergate.staff_engagement.interaction.domain.InteractionType;
import com.psybergate.staff_engagement.scheduling.service.NextScheduledInteractionService;
import com.psybergate.staff_engagement.task.domain.TaskRepository;
import com.psybergate.staff_engagement.user.domain.User;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import org.mockito.Mockito;

/**
 * Property-based test: Interaction ordering is descending by occurredAt.
 *
 * For any list of interactions belonging to an employee, the Employee360Service
 * SHALL return them ordered such that for every consecutive pair (i, i+1),
 * interactions[i].occurredAt >= interactions[i+1].occurredAt.
 *
 * Validates: Requirements 1.2, 2.2
 */
class Employee360InteractionOrderPropertyTest {

	private final EmployeeRepository employeeRepository = Mockito.mock(EmployeeRepository.class);
	private final InteractionRepository interactionRepository = Mockito.mock(InteractionRepository.class);
	private final TaskRepository taskRepository = Mockito.mock(TaskRepository.class);
	private final NextScheduledInteractionService nextScheduledInteractionService = Mockito.mock(NextScheduledInteractionService.class);
	private final Employee360Service service = new Employee360ServiceImpl(employeeRepository, interactionRepository, taskRepository, nextScheduledInteractionService);

	@Property(tries = 100)
	@Tag("Feature: employee-360-view, Property 2: Interaction ordering is descending by occurredAt")
	void interactionOrderingIsDescendingByOccurredAt(
			@ForAll("interactionInstants") List<Instant> instants) {

		// Arrange: create a valid employee
		Employee employee = new Employee();
		employee.setId(1L);
		employee.setName("Test Employee");
		employee.setEmail("test@example.com");
		employee.setJobTitle("Developer");
		employee.setCreatedAt(Instant.now());

		User conductedBy = new User();
		conductedBy.setId(1L);
		conductedBy.setName("Conductor");
		conductedBy.setEmail("conductor@example.com");
		conductedBy.setCreatedAt(Instant.now());

		// Build interactions with given instants, sort descending (as the repository would)
		List<Interaction> interactions = IntStream.range(0, instants.size())
				.mapToObj(i -> {
					Interaction interaction = new Interaction();
					interaction.setId((long) (i + 1));
					interaction.setEmployee(employee);
					interaction.setConductedBy(conductedBy);
					interaction.setLoggedBy(conductedBy);
					interaction.setType(InteractionType.CHECK_IN);
					interaction.setNotes("Note " + i);
					interaction.setOccurredAt(instants.get(i));
					interaction.setCreatedAt(Instant.now());
					return interaction;
				})
				.sorted(Comparator.comparing(Interaction::getOccurredAt).reversed())
				.toList();

		// Mock repository behaviour
		when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
		when(interactionRepository.findByEmployeeIdOrderByOccurredAtDesc(1L)).thenReturn(interactions);
		when(taskRepository.findByInteractionIdInAndStatus(any(), any())).thenReturn(List.of());

		// Act
		Employee360Response response = service.getEmployee360(1L);

		// Assert: each consecutive pair satisfies occurredAt[i] >= occurredAt[i+1]
		List<InteractionDto> result = response.interactions();
		for (int i = 0; i < result.size() - 1; i++) {
			assertThat(result.get(i).occurredAt())
					.as("interactions[%d].occurredAt should be >= interactions[%d].occurredAt", i, i + 1)
					.isAfterOrEqualTo(result.get(i + 1).occurredAt());
		}
	}

	/**
	 * Generates lists of 0-20 random Instant values spanning a wide range.
	 */
	@Provide
	Arbitrary<List<Instant>> interactionInstants() {
		Arbitrary<Instant> instantArbitrary = Arbitraries.longs()
				.between(0L, 4_102_444_800L) // epoch seconds: 2000-01-01 to 2100-01-01
				.map(Instant::ofEpochSecond);

		return instantArbitrary.list().ofMinSize(0).ofMaxSize(20);
	}
}
