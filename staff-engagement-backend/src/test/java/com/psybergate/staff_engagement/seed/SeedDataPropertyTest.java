package com.psybergate.staff_engagement.seed;

import com.psybergate.staff_engagement.BaseIntegrationTest;
import com.psybergate.staff_engagement.employee.Employee;
import com.psybergate.staff_engagement.employee.EmployeeRepository;
import com.psybergate.staff_engagement.interaction.Interaction;
import com.psybergate.staff_engagement.interaction.InteractionRepository;
import com.psybergate.staff_engagement.interaction.InteractionType;
import com.psybergate.staff_engagement.scheduling.CompletionStatus;
import com.psybergate.staff_engagement.scheduling.ScheduledInteraction;
import com.psybergate.staff_engagement.scheduling.ScheduledInteractionRepository;
import com.psybergate.staff_engagement.task.Task;
import com.psybergate.staff_engagement.task.TaskRepository;
import com.psybergate.staff_engagement.task.TaskStatus;
import com.psybergate.staff_engagement.user.User;
import com.psybergate.staff_engagement.user.UserRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests validating seed data distribution constraints.
 * These tests verify universal properties hold across all seeded records.
 */
class SeedDataPropertyTest extends BaseIntegrationTest {

	@Autowired
	private InteractionRepository interactionRepository;

	@Autowired
	private EmployeeRepository employeeRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TaskRepository taskRepository;

	@Autowired
	private ScheduledInteractionRepository scheduledInteractionRepository;

	@Autowired
	private SeedDataLoader seedDataLoader;

	/**
	 * Property 1: Interaction type distribution per employee
	 *
	 * For any new employee in the seeded dataset, the distribution of interaction types
	 * across that employee's 20 interactions SHALL include at least 3 of the 4 defined types
	 * (CHECK_IN, MENTORING, CATCH_UP, OTHER) and no single type SHALL account for more than
	 * 10 interactions.
	 *
	 * Validates: Requirements 3.2
	 */
	@Test
	@Tag("Feature: add-test-seed-data, Property 1: Interaction type distribution per employee")
	void interactionTypeDistributionPerEmployee() {
		// Get the 20 new employees (those with @company.com emails)
		List<Employee> newEmployees = employeeRepository.findAll().stream()
				.filter(e -> e.getEmail().endsWith("@company.com"))
				.toList();

		assertThat(newEmployees).hasSize(20);

		for (Employee emp : newEmployees) {
			List<Interaction> interactions = interactionRepository
					.findByEmployeeIdOrderByOccurredAtDesc(emp.getId());
			assertThat(interactions)
					.as("Employee %s should have exactly 20 interactions", emp.getName())
					.hasSize(20);

			Map<InteractionType, Long> typeCounts = interactions.stream()
					.collect(Collectors.groupingBy(Interaction::getType, Collectors.counting()));

			// At least 3 of 4 types represented
			assertThat(typeCounts.keySet())
					.as("Employee %s should have at least 3 of 4 interaction types", emp.getName())
					.hasSizeGreaterThanOrEqualTo(3);

			// No type more than 10
			typeCounts.forEach((type, count) ->
					assertThat(count)
							.as("Employee %s should not have more than 10 interactions of type %s",
									emp.getName(), type)
							.isLessThanOrEqualTo(10));
		}
	}

	/**
	 * Property 2: User assignment distribution per employee
	 *
	 * For any new employee in the seeded dataset, at least 3 distinct users SHALL appear
	 * as conducted_by_user_id across that employee's 20 interactions, and at least 3 distinct
	 * users SHALL appear as logged_by_user_id.
	 *
	 * Validates: Requirements 3.3
	 */
	@Test
	@Tag("Feature: add-test-seed-data, Property 2: User assignment distribution per employee")
	void userAssignmentDistributionPerEmployee() {
		List<Employee> newEmployees = employeeRepository.findAll().stream()
				.filter(e -> e.getEmail().endsWith("@company.com"))
				.toList();

		assertThat(newEmployees).isNotEmpty();

		for (Employee emp : newEmployees) {
			List<Interaction> interactions = interactionRepository.findAll().stream()
					.filter(i -> i.getEmployee().getId().equals(emp.getId()))
					.toList();

			long distinctConductedBy = interactions.stream()
					.map(i -> i.getConductedBy().getId())
					.distinct().count();
			long distinctLoggedBy = interactions.stream()
					.map(i -> i.getLoggedBy().getId())
					.distinct().count();

			assertThat(distinctConductedBy)
					.as("Employee %s should have ≥3 distinct conductedBy users", emp.getName())
					.isGreaterThanOrEqualTo(3);
			assertThat(distinctLoggedBy)
					.as("Employee %s should have ≥3 distinct loggedBy users", emp.getName())
					.isGreaterThanOrEqualTo(3);
		}
	}

	/**
	 * Property 3: Temporal spread of interactions per employee.
	 *
	 * For any new employee in the seeded dataset, the occurred_at timestamps across that
	 * employee's 20 interactions SHALL span at least 8 distinct calendar months within the
	 * 12 months preceding the seed execution date.
	 *
	 * Validates: Requirements 3.4
	 */
	@Test
	@Tag("Feature: add-test-seed-data, Property 3: Temporal spread of interactions per employee")
	void temporalSpreadOfInteractionsPerEmployee() {
		List<Employee> newEmployees = employeeRepository.findAll().stream()
				.filter(e -> e.getEmail().endsWith("@company.com"))
				.toList();

		assertThat(newEmployees).isNotEmpty();

		for (Employee emp : newEmployees) {
			List<Interaction> interactions = interactionRepository.findAll().stream()
					.filter(i -> i.getEmployee().getId().equals(emp.getId()))
					.toList();

			long distinctMonths = interactions.stream()
					.map(i -> YearMonth.from(i.getOccurredAt().atZone(ZoneId.systemDefault())))
					.distinct()
					.count();

			assertThat(distinctMonths)
					.as("Employee %s should have interactions spanning ≥8 distinct months", emp.getName())
					.isGreaterThanOrEqualTo(8);
		}
	}

	/**
	 * Property 4: Project assignment coverage per employee
	 *
	 * For any new employee in the seeded dataset, at least 6 of that employee's
	 * 20 interactions SHALL have a non-null project reference.
	 *
	 * Validates: Requirements 3.5
	 */
	@Test
	@Tag("Feature: add-test-seed-data, Property 4: Project assignment coverage per employee")
	void projectAssignmentCoveragePerEmployee() {
		List<Employee> newEmployees = employeeRepository.findAll().stream()
				.filter(e -> e.getEmail().endsWith("@company.com"))
				.toList();

		assertThat(newEmployees).isNotEmpty();

		for (Employee emp : newEmployees) {
			List<Interaction> interactions = interactionRepository.findAll().stream()
					.filter(i -> i.getEmployee().getId().equals(emp.getId()))
					.toList();

			long withProject = interactions.stream()
					.filter(i -> i.getProject() != null)
					.count();

			assertThat(withProject)
					.as("Employee %s should have at least 6 interactions with a project", emp.getName())
					.isGreaterThanOrEqualTo(6);
		}
	}

	/**
	 * Property 5: Interaction notes uniqueness and length
	 *
	 * For any pair of interactions in the 400 new interaction records, their notes fields
	 * SHALL be distinct. Additionally, for any single interaction, the notes field SHALL
	 * contain between 20 and 200 characters.
	 *
	 * Validates: Requirements 3.6
	 */
	@Test
	@Tag("Feature: add-test-seed-data, Property 5: Interaction notes uniqueness and length")
	void interactionNotesUniquenessAndLength() {
		// Get only the 400 new interactions (those belonging to @company.com employees)
		List<Employee> newEmployees = employeeRepository.findAll().stream()
				.filter(e -> e.getEmail().endsWith("@company.com"))
				.toList();
		Set<Long> newEmployeeIds = newEmployees.stream().map(Employee::getId).collect(Collectors.toSet());

		List<Interaction> newInteractions = interactionRepository.findAll().stream()
				.filter(i -> newEmployeeIds.contains(i.getEmployee().getId()))
				.toList();

		assertThat(newInteractions).hasSize(400);

		// All notes distinct
		Set<String> uniqueNotes = newInteractions.stream()
				.map(Interaction::getNotes)
				.collect(Collectors.toSet());
		assertThat(uniqueNotes).hasSize(400);

		// Each note between 20–200 characters
		for (Interaction interaction : newInteractions) {
			String notes = interaction.getNotes();
			assertThat(notes.length()).isBetween(20, 200);
		}
	}

	/**
	 * Property 6: Task status distribution per employee
	 *
	 * For any employee among the 5 employees receiving new tasks, that employee SHALL have
	 * at least 2 tasks with status OPEN and at least 1 task with status DONE.
	 *
	 * Validates: Requirements 4.2
	 */
	@Test
	@Tag("Feature: add-test-seed-data, Property 6: Task status distribution per employee")
	void taskStatusDistributionPerEmployee() {
		// Get employees that have tasks (first 5 new employees)
		List<Employee> newEmployees = employeeRepository.findAll().stream()
				.filter(e -> e.getEmail().endsWith("@company.com"))
				.toList();

		// Only first 5 get tasks
		List<Employee> employeesWithTasks = newEmployees.stream().limit(5).toList();

		for (Employee emp : employeesWithTasks) {
			List<Task> tasks = taskRepository.findAll().stream()
					.filter(t -> t.getEmployee() != null && t.getEmployee().getId().equals(emp.getId()))
					.toList();

			long openCount = tasks.stream().filter(t -> t.getStatus() == TaskStatus.OPEN).count();
			long doneCount = tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();

			assertThat(openCount).as("Employee %s should have ≥2 OPEN tasks", emp.getName())
					.isGreaterThanOrEqualTo(2);
			assertThat(doneCount).as("Employee %s should have ≥1 DONE task", emp.getName())
					.isGreaterThanOrEqualTo(1);
		}
	}

	/**
	 * Property 7: Task due date spread per employee
	 *
	 * For any employee among the 5 employees receiving new tasks, that employee SHALL have
	 * at least 1 task with a due_date before today and at least 1 task with a due_date on
	 * or after today.
	 *
	 * Validates: Requirements 4.3
	 */
	@Test
	@Tag("Feature: add-test-seed-data, Property 7: Task due date spread per employee")
	void taskDueDateSpreadPerEmployee() {
		List<Employee> newEmployees = employeeRepository.findAll().stream()
				.filter(e -> e.getEmail().endsWith("@company.com"))
				.toList();

		List<Employee> employeesWithTasks = newEmployees.stream().limit(5).toList();
		LocalDate today = LocalDate.now();

		for (Employee emp : employeesWithTasks) {
			List<Task> tasks = taskRepository.findAll().stream()
					.filter(t -> t.getEmployee() != null && t.getEmployee().getId().equals(emp.getId()))
					.toList();

			long pastDueDates = tasks.stream()
					.filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(today))
					.count();
			long futureDueDates = tasks.stream()
					.filter(t -> t.getDueDate() != null && !t.getDueDate().isBefore(today))
					.count();

			assertThat(pastDueDates).as("Employee %s should have ≥1 task with past due date", emp.getName())
					.isGreaterThanOrEqualTo(1);
			assertThat(futureDueDates).as("Employee %s should have ≥1 task with future due date", emp.getName())
					.isGreaterThanOrEqualTo(1);
		}
	}

	/**
	 * Property 10: Scheduled interaction type diversity per user
	 *
	 * For any user among the 5 seeded users, the 3 scheduled interactions assigned to that
	 * user SHALL include at least 2 different interaction types.
	 *
	 * Validates: Requirements 5.2
	 */
	@Test
	@Tag("Feature: add-test-seed-data, Property 10: Scheduled interaction type diversity per user")
	void scheduledInteractionTypeDiversityPerUser() {
		List<User> allUsers = userRepository.findAll();
		List<ScheduledInteraction> allScheduled = scheduledInteractionRepository.findAll();

		for (User user : allUsers) {
			List<ScheduledInteraction> userScheduled = allScheduled.stream()
					.filter(s -> s.getScheduledBy().getId().equals(user.getId()))
					.toList();

			assertThat(userScheduled).as("User %s should have 3 scheduled interactions", user.getName())
					.hasSize(3);

			long distinctTypes = userScheduled.stream()
					.map(ScheduledInteraction::getInteractionType)
					.distinct().count();

			assertThat(distinctTypes).as("User %s should have ≥2 different interaction types", user.getName())
					.isGreaterThanOrEqualTo(2);
		}
	}

	/**
	 * Property 11: Scheduled interaction status diversity per user
	 *
	 * For any user among the 5 seeded users, the 3 scheduled interactions assigned to that
	 * user SHALL include at least 2 different completion statuses.
	 *
	 * Validates: Requirements 5.3
	 */
	@Test
	@Tag("Feature: add-test-seed-data, Property 11: Scheduled interaction status diversity per user")
	void scheduledInteractionStatusDiversityPerUser() {
		List<User> allUsers = userRepository.findAll();
		List<ScheduledInteraction> allScheduled = scheduledInteractionRepository.findAll();

		for (User user : allUsers) {
			List<ScheduledInteraction> userScheduled = allScheduled.stream()
					.filter(s -> s.getScheduledBy().getId().equals(user.getId()))
					.toList();

			long distinctStatuses = userScheduled.stream()
					.map(ScheduledInteraction::getCompletionStatus)
					.distinct().count();

			assertThat(distinctStatuses).as("User %s should have ≥2 different completion statuses", user.getName())
					.isGreaterThanOrEqualTo(2);
		}
	}

	/**
	 * Property 12: Scheduled interaction date-status consistency
	 *
	 * For any scheduled interaction record, if its completion status is COMPLETED or CANCELLED
	 * then its scheduled_date SHALL be in the past (before today), and if its completion status
	 * is PENDING then its scheduled_date SHALL be in the future (today or later).
	 *
	 * Validates: Requirements 5.5, 5.7, 7.3
	 */
	@Test
	@Tag("Feature: add-test-seed-data, Property 12: Scheduled interaction date-status consistency")
	void scheduledInteractionDateStatusConsistency() {
		List<ScheduledInteraction> allScheduled = scheduledInteractionRepository.findAll();
		LocalDate today = LocalDate.now();

		assertThat(allScheduled).hasSize(15);

		for (ScheduledInteraction scheduled : allScheduled) {
			CompletionStatus status = scheduled.getCompletionStatus();
			LocalDate scheduledDate = scheduled.getScheduledDate();

			if (status == CompletionStatus.PENDING) {
				assertThat(scheduledDate)
						.as("PENDING scheduled interaction should have a future date")
						.isAfterOrEqualTo(today);
			} else {
				// COMPLETED or CANCELLED
				assertThat(scheduledDate)
						.as("COMPLETED/CANCELLED scheduled interaction should have a past date")
						.isBefore(today);
			}
		}
	}

	/**
	 * Property 13: Scheduled interaction notes uniqueness and length
	 *
	 * For any pair of scheduled interactions in the 15 new records, their notes fields
	 * SHALL be distinct. Additionally, for any single scheduled interaction, the notes field
	 * SHALL contain between 10 and 200 characters.
	 *
	 * Validates: Requirements 5.6
	 */
	@Test
	@Tag("Feature: add-test-seed-data, Property 13: Scheduled interaction notes uniqueness and length")
	void scheduledInteractionNotesUniquenessAndLength() {
		List<ScheduledInteraction> allScheduled = scheduledInteractionRepository.findAll();

		assertThat(allScheduled).hasSize(15);

		// All notes distinct
		Set<String> uniqueNotes = allScheduled.stream()
				.map(ScheduledInteraction::getNotes)
				.collect(Collectors.toSet());
		assertThat(uniqueNotes).hasSize(15);

		// Each note between 10–200 characters
		for (ScheduledInteraction scheduled : allScheduled) {
			String notes = scheduled.getNotes();
			assertThat(notes).isNotNull();
			assertThat(notes.length()).isBetween(10, 200);
		}
	}

	/**
	 * Property 8: Referential integrity for all new records
	 *
	 * For any new record created by the seed loader (Task, Interaction, or ScheduledInteraction),
	 * every foreign key field SHALL reference an existing record in the corresponding parent table
	 * at the time of verification.
	 *
	 * Validates: Requirements 4.4, 4.5, 7.1
	 */
	@Test
	@Tag("Feature: add-test-seed-data, Property 8: Referential integrity for all new records")
	void referentialIntegrityForAllNewRecords() {
		Set<Long> allEmployeeIds = employeeRepository.findAll().stream()
				.map(Employee::getId).collect(Collectors.toSet());
		Set<Long> allUserIds = userRepository.findAll().stream()
				.map(User::getId).collect(Collectors.toSet());

		// Check Interactions FK integrity
		List<Interaction> allInteractions = interactionRepository.findAll();
		for (Interaction interaction : allInteractions) {
			assertThat(allEmployeeIds).contains(interaction.getEmployee().getId());
			assertThat(allUserIds).contains(interaction.getConductedBy().getId());
			assertThat(allUserIds).contains(interaction.getLoggedBy().getId());
		}

		// Check Tasks FK integrity
		List<Task> allTasks = taskRepository.findAll();
		for (Task task : allTasks) {
			if (task.getInteraction() != null) {
				assertThat(interactionRepository.findById(task.getInteraction().getId())).isPresent();
			}
			if (task.getAssignedUser() != null) {
				assertThat(allUserIds).contains(task.getAssignedUser().getId());
			}
			if (task.getEmployee() != null) {
				assertThat(allEmployeeIds).contains(task.getEmployee().getId());
			}
		}

		// Check ScheduledInteractions FK integrity
		List<ScheduledInteraction> allScheduled = scheduledInteractionRepository.findAll();
		for (ScheduledInteraction scheduled : allScheduled) {
			assertThat(allEmployeeIds).contains(scheduled.getEmployee().getId());
			assertThat(allUserIds).contains(scheduled.getScheduledBy().getId());
		}
	}

	/**
	 * Property 15: DONE task due date consistency
	 *
	 * For any task with status DONE, its due_date SHALL be either null or a date in the past
	 * (before today).
	 *
	 * Validates: Requirements 7.4
	 */
	@Test
	@Tag("Feature: add-test-seed-data, Property 15: DONE task due date consistency")
	void doneTaskDueDateConsistency() {
		List<Task> allTasks = taskRepository.findAll();
		LocalDate today = LocalDate.now();

		List<Task> doneTasks = allTasks.stream()
				.filter(t -> t.getStatus() == TaskStatus.DONE)
				.toList();

		assertThat(doneTasks).isNotEmpty();

		for (Task task : doneTasks) {
			if (task.getDueDate() != null) {
				assertThat(task.getDueDate())
						.as("DONE task '%s' should have a past or null due date", task.getTitle())
						.isBefore(today);
			}
		}
	}

	/**
	 * Property 9: Task title and description constraints
	 *
	 * For any pair of tasks in the 25 new task records, their titles SHALL be distinct.
	 * Additionally, for any single task, the title SHALL be between 1 and 255 characters,
	 * and the description SHALL be between 1 and 2000 characters.
	 *
	 * Validates: Requirements 4.6
	 */
	@Test
	@Tag("Feature: add-test-seed-data, Property 9: Task title and description constraints")
	void taskTitleAndDescriptionConstraints() {
		// Get new tasks (those linked to @company.com employees)
		Set<Long> newEmployeeIds = employeeRepository.findAll().stream()
				.filter(e -> e.getEmail().endsWith("@company.com"))
				.map(Employee::getId)
				.collect(Collectors.toSet());

		List<Task> newTasks = taskRepository.findAll().stream()
				.filter(t -> t.getEmployee() != null && newEmployeeIds.contains(t.getEmployee().getId()))
				.toList();

		assertThat(newTasks).hasSize(25);

		// All titles distinct
		Set<String> uniqueTitles = newTasks.stream().map(Task::getTitle).collect(Collectors.toSet());
		assertThat(uniqueTitles).hasSize(25);

		// Title 1–255 chars, description 1–2000 chars
		for (Task task : newTasks) {
			assertThat(task.getTitle().length()).isBetween(1, 255);
			assertThat(task.getDescription().length()).isBetween(1, 2000);
		}
	}

	/**
	 * Property 14: Interaction temporal ordering
	 *
	 * For any new interaction record, its occurred_at timestamp SHALL precede the current time,
	 * and its created_at timestamp SHALL be equal to or later than its occurred_at value.
	 *
	 * Validates: Requirements 7.2
	 */
	@Test
	@Tag("Feature: add-test-seed-data, Property 14: Interaction temporal ordering")
	void interactionTemporalOrdering() {
		List<Interaction> allInteractions = interactionRepository.findAll();
		Instant now = Instant.now();

		for (Interaction interaction : allInteractions) {
			assertThat(interaction.getOccurredAt())
					.as("Interaction occurred_at should be before now")
					.isBefore(now);

			if (interaction.getCreatedAt() != null) {
				assertThat(interaction.getCreatedAt())
						.as("Interaction created_at should be >= occurred_at")
						.isAfterOrEqualTo(interaction.getOccurredAt());
			}
		}
	}

	/**
	 * Property 16: Seed loader idempotency
	 *
	 * For any number of consecutive executions of the seed loader on an already-seeded database,
	 * the total row counts across all seeded tables SHALL remain unchanged after each execution.
	 *
	 * Validates: Requirements 6.4
	 */
	@Test
	@Tag("Feature: add-test-seed-data, Property 16: Seed loader idempotency")
	void seedLoaderIdempotency() {
		// Seed data is already loaded via SeedDataLoader running on context startup
		// Capture counts after first run
		long userCount = userRepository.count();
		long employeeCount = employeeRepository.count();
		long interactionCount = interactionRepository.count();
		long taskCount = taskRepository.count();
		long scheduledCount = scheduledInteractionRepository.count();

		// Run seed loader again
		seedDataLoader.run(null);

		// Assert counts unchanged
		assertThat(userRepository.count()).isEqualTo(userCount);
		assertThat(employeeRepository.count()).isEqualTo(employeeCount);
		assertThat(interactionRepository.count()).isEqualTo(interactionCount);
		assertThat(taskRepository.count()).isEqualTo(taskCount);
		assertThat(scheduledInteractionRepository.count()).isEqualTo(scheduledCount);
	}
}
