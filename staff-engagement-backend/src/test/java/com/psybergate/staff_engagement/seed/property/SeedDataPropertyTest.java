package com.psybergate.staff_engagement.seed.property;

import static org.assertj.core.api.Assertions.assertThat;

import com.psybergate.staff_engagement.employee.domain.Employee;
import com.psybergate.staff_engagement.employee.domain.EmployeeRepository;
import com.psybergate.staff_engagement.interaction.domain.Interaction;
import com.psybergate.staff_engagement.interaction.domain.InteractionRepository;
import com.psybergate.staff_engagement.interaction.domain.InteractionType;
import com.psybergate.staff_engagement.scheduling.domain.CompletionStatus;
import com.psybergate.staff_engagement.scheduling.domain.ScheduledInteraction;
import com.psybergate.staff_engagement.scheduling.domain.ScheduledInteractionRepository;
import com.psybergate.staff_engagement.seed.SeedDataLoader;
import com.psybergate.staff_engagement.support.BaseIntegrationTest;
import com.psybergate.staff_engagement.task.domain.Task;
import com.psybergate.staff_engagement.task.domain.TaskRepository;
import com.psybergate.staff_engagement.task.domain.TaskStatus;
import com.psybergate.staff_engagement.user.domain.User;
import com.psybergate.staff_engagement.user.domain.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
	 * Property 1: Interaction type diversity
	 *
	 * The seeded interactions SHALL include at least 3 of the 4 defined interaction types
	 * (CHECK_IN, MENTORING, CATCH_UP, OTHER).
	 */
	@Test
	@Tag("seed-data")
	void interactionTypeDiversityAcrossDataset() {
		List<Interaction> allInteractions = interactionRepository.findAll();

		assertThat(allInteractions).isNotEmpty();

		Set<InteractionType> types = allInteractions.stream()
				.map(Interaction::getType)
				.collect(Collectors.toSet());

		assertThat(types)
				.as("Seed data should include at least 3 of the 4 interaction types")
				.hasSizeGreaterThanOrEqualTo(3);
	}

	/**
	 * Property 2: Multiple users conduct and log interactions
	 *
	 * The seeded interactions SHALL be conducted/logged by at least 3 distinct users.
	 */
	@Test
	@Tag("seed-data")
	void multipleUsersConductAndLogInteractions() {
		List<Interaction> allInteractions = interactionRepository.findAll();

		long distinctConductedBy = allInteractions.stream()
				.map(i -> i.getConductedBy().getId())
				.distinct().count();
		long distinctLoggedBy = allInteractions.stream()
				.map(i -> i.getLoggedBy().getId())
				.distinct().count();

		assertThat(distinctConductedBy)
				.as("At least 3 distinct users should conduct interactions")
				.isGreaterThanOrEqualTo(3);
		assertThat(distinctLoggedBy)
				.as("At least 3 distinct users should log interactions")
				.isGreaterThanOrEqualTo(3);
	}

	/**
	 * Property 3: Temporal spread of interactions
	 *
	 * The seeded interactions SHALL span at least 30 days to provide meaningful engagement
	 * status distribution (ON_TRACK, AT_RISK, OVERDUE).
	 */
	@Test
	@Tag("seed-data")
	void temporalSpreadOfInteractions() {
		List<Interaction> allInteractions = interactionRepository.findAll();

		assertThat(allInteractions).isNotEmpty();

		Instant earliest = allInteractions.stream()
				.map(Interaction::getOccurredAt)
				.min(Instant::compareTo)
				.orElseThrow();
		Instant latest = allInteractions.stream()
				.map(Interaction::getOccurredAt)
				.max(Instant::compareTo)
				.orElseThrow();

		long daysBetween = java.time.Duration.between(earliest, latest).toDays();

		assertThat(daysBetween)
				.as("Interactions should span at least 30 days for engagement status diversity")
				.isGreaterThanOrEqualTo(30);
	}

	/**
	 * Property 4: Some interactions have project references
	 *
	 * At least some interactions SHALL reference a project (non-null project_id).
	 */
	@Test
	@Tag("seed-data")
	void someInteractionsHaveProjectReferences() {
		List<Interaction> allInteractions = interactionRepository.findAll();

		long withProject = allInteractions.stream()
				.filter(i -> i.getProject() != null)
				.count();

		assertThat(withProject)
				.as("At least some interactions should reference a project")
				.isGreaterThanOrEqualTo(1);
	}

	/**
	 * Property 5: Interaction notes are non-empty and within length constraints
	 *
	 * For any interaction, the notes field SHALL be non-empty and between 10 and 2000 characters.
	 */
	@Test
	@Tag("seed-data")
	void interactionNotesLengthConstraints() {
		List<Interaction> allInteractions = interactionRepository.findAll();

		assertThat(allInteractions).isNotEmpty();

		for (Interaction interaction : allInteractions) {
			String notes = interaction.getNotes();
			assertThat(notes).isNotNull().isNotEmpty();
			assertThat(notes.length())
					.as("Notes should be between 10 and 2000 characters")
					.isBetween(10, 2000);
		}
	}

	/**
	 * Property 6: Task status distribution
	 *
	 * The seeded tasks SHALL include at least 1 OPEN task and at least 1 DONE task.
	 */
	@Test
	@Tag("seed-data")
	void taskStatusDistribution() {
		List<Task> allTasks = taskRepository.findAll();

		long openCount = allTasks.stream().filter(t -> t.getStatus() == TaskStatus.OPEN).count();
		long doneCount = allTasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();

		assertThat(openCount).as("Should have at least 1 OPEN task").isGreaterThanOrEqualTo(1);
		assertThat(doneCount).as("Should have at least 1 DONE task").isGreaterThanOrEqualTo(1);
	}

	/**
	 * Property 7: Task due date spread
	 *
	 * The seeded tasks SHALL include at least 1 task with a due_date before today
	 * and at least 1 task with a due_date on or after today.
	 */
	@Test
	@Tag("seed-data")
	void taskDueDateSpread() {
		List<Task> allTasks = taskRepository.findAll();
		LocalDate today = LocalDate.now();

		long pastDueDates = allTasks.stream()
				.filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(today))
				.count();
		long futureDueDates = allTasks.stream()
				.filter(t -> t.getDueDate() != null && !t.getDueDate().isBefore(today))
				.count();

		assertThat(pastDueDates).as("Should have at least 1 task with past due date")
				.isGreaterThanOrEqualTo(1);
		assertThat(futureDueDates).as("Should have at least 1 task with future due date")
				.isGreaterThanOrEqualTo(1);
	}

	/**
	 * Property 8: Referential integrity for all records
	 *
	 * Every foreign key field SHALL reference an existing record in the corresponding
	 * parent table.
	 */
	@Test
	@Tag("seed-data")
	void referentialIntegrityForAllRecords() {
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
	 * Property 9: Task title and description constraints
	 *
	 * For any task, the title SHALL be between 1 and 255 characters,
	 * and the description SHALL be between 1 and 2000 characters.
	 */
	@Test
	@Tag("seed-data")
	void taskTitleAndDescriptionConstraints() {
		List<Task> allTasks = taskRepository.findAll();

		assertThat(allTasks).isNotEmpty();

		for (Task task : allTasks) {
			assertThat(task.getTitle().length()).isBetween(1, 255);
			assertThat(task.getDescription()).isNotNull();
			assertThat(task.getDescription().length()).isBetween(1, 2000);
		}
	}

	/**
	 * Property 10: Scheduled interaction type diversity
	 *
	 * The seeded scheduled interactions SHALL include at least 2 different interaction types.
	 */
	@Test
	@Tag("seed-data")
	void scheduledInteractionTypeDiversity() {
		List<ScheduledInteraction> allScheduled = scheduledInteractionRepository.findAll();

		assertThat(allScheduled).isNotEmpty();

		long distinctTypes = allScheduled.stream()
				.map(ScheduledInteraction::getInteractionType)
				.distinct().count();

		assertThat(distinctTypes)
				.as("Scheduled interactions should have at least 2 different types")
				.isGreaterThanOrEqualTo(2);
	}

	/**
	 * Property 11: Scheduled interaction status diversity
	 *
	 * The seeded scheduled interactions SHALL include all 3 completion statuses
	 * (PENDING, COMPLETED, CANCELLED).
	 */
	@Test
	@Tag("seed-data")
	void scheduledInteractionStatusDiversity() {
		List<ScheduledInteraction> allScheduled = scheduledInteractionRepository.findAll();

		Set<CompletionStatus> statuses = allScheduled.stream()
				.map(ScheduledInteraction::getCompletionStatus)
				.collect(Collectors.toSet());

		assertThat(statuses)
				.as("Scheduled interactions should include all 3 completion statuses")
				.containsExactlyInAnyOrder(
						CompletionStatus.PENDING,
						CompletionStatus.COMPLETED,
						CompletionStatus.CANCELLED);
	}

	/**
	 * Property 12: Scheduled interaction date-status consistency
	 *
	 * If completion status is COMPLETED or CANCELLED then scheduled_date SHALL be in the past.
	 * If completion status is PENDING then scheduled_date SHALL be today or in the future.
	 */
	@Test
	@Tag("seed-data")
	void scheduledInteractionDateStatusConsistency() {
		List<ScheduledInteraction> allScheduled = scheduledInteractionRepository.findAll();
		LocalDate today = LocalDate.now();

		assertThat(allScheduled).isNotEmpty();

		for (ScheduledInteraction scheduled : allScheduled) {
			CompletionStatus status = scheduled.getCompletionStatus();
			LocalDate scheduledDate = scheduled.getScheduledDate();

			if (status == CompletionStatus.PENDING) {
				assertThat(scheduledDate)
						.as("PENDING scheduled interaction should have a future date")
						.isAfterOrEqualTo(today);
			} else {
				assertThat(scheduledDate)
						.as("COMPLETED/CANCELLED scheduled interaction should have a past date")
						.isBefore(today);
			}
		}
	}

	/**
	 * Property 13: Scheduled interaction notes constraints
	 *
	 * For any scheduled interaction, the notes field SHALL be non-null and between
	 * 10 and 2000 characters.
	 */
	@Test
	@Tag("seed-data")
	void scheduledInteractionNotesConstraints() {
		List<ScheduledInteraction> allScheduled = scheduledInteractionRepository.findAll();

		assertThat(allScheduled).isNotEmpty();

		for (ScheduledInteraction scheduled : allScheduled) {
			String notes = scheduled.getNotes();
			assertThat(notes).isNotNull();
			assertThat(notes.length()).isBetween(10, 2000);
		}
	}

	/**
	 * Property 14: Interaction temporal ordering
	 *
	 * For any interaction, its occurred_at timestamp SHALL precede the current time,
	 * and its created_at timestamp SHALL be equal to or later than its occurred_at value.
	 */
	@Test
	@Tag("seed-data")
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
	 * Property 15: DONE task due date consistency
	 *
	 * For any task with status DONE, its due_date SHALL be either null or a date in the past.
	 */
	@Test
	@Tag("seed-data")
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
	 * Property 16: Seed loader idempotency
	 *
	 * Running the seed loader multiple times SHALL not create duplicate records.
	 */
	@Test
	@Tag("seed-data")
	void seedLoaderIdempotency() {
		long userCount = userRepository.count();
		long employeeCount = employeeRepository.count();
		long interactionCount = interactionRepository.count();
		long taskCount = taskRepository.count();
		long scheduledCount = scheduledInteractionRepository.count();

		// Run seed loader again
		seedDataLoader.run(null);

		assertThat(userRepository.count()).isEqualTo(userCount);
		assertThat(employeeRepository.count()).isEqualTo(employeeCount);
		assertThat(interactionRepository.count()).isEqualTo(interactionCount);
		assertThat(taskRepository.count()).isEqualTo(taskCount);
		assertThat(scheduledInteractionRepository.count()).isEqualTo(scheduledCount);
	}

	/**
	 * Property 17: Minimum data volume for demo
	 *
	 * The seed data SHALL include enough records to demonstrate all features:
	 * at least 4 users, 10 employees, 10 interactions, 10 tasks, and 10 scheduled interactions.
	 */
	@Test
	@Tag("seed-data")
	void minimumDataVolumeForDemo() {
		assertThat(userRepository.count())
				.as("At least 4 users for login/demo")
				.isGreaterThanOrEqualTo(4);
		assertThat(employeeRepository.count())
				.as("At least 10 employees for engagement list")
				.isGreaterThanOrEqualTo(10);
		assertThat(interactionRepository.count())
				.as("At least 10 interactions for history")
				.isGreaterThanOrEqualTo(10);
		assertThat(taskRepository.count())
				.as("At least 10 tasks for task management")
				.isGreaterThanOrEqualTo(10);
		assertThat(scheduledInteractionRepository.count())
				.as("At least 10 scheduled interactions for scheduling view")
				.isGreaterThanOrEqualTo(10);
	}

	/**
	 * Property 18: Engagement status coverage
	 *
	 * Based on the engagement thresholds (at-risk: 14 days, overdue: 30 days),
	 * the seed data SHALL produce employees in all three engagement states.
	 * This is verified by checking that the most recent interaction dates for employees
	 * span all three ranges.
	 */
	@Test
	@Tag("seed-data")
	void engagementStatusCoverage() {
		List<Employee> allEmployees = employeeRepository.findAll();
		List<Interaction> allInteractions = interactionRepository.findAll();
		Instant now = Instant.now();

		boolean hasOnTrack = false;
		boolean hasAtRisk = false;
		boolean hasOverdue = false;

		for (Employee emp : allEmployees) {
			allInteractions.stream()
					.filter(i -> i.getEmployee().getId().equals(emp.getId()))
					.map(Interaction::getOccurredAt)
					.max(Instant::compareTo)
					.ifPresent(lastInteraction -> {
						// Not modifying outer booleans here — using a different approach below
					});
		}

		// Recompute using a simpler approach
		for (Employee emp : allEmployees) {
			var lastInteraction = allInteractions.stream()
					.filter(i -> i.getEmployee().getId().equals(emp.getId()))
					.map(Interaction::getOccurredAt)
					.max(Instant::compareTo);

			if (lastInteraction.isEmpty()) {
				hasOverdue = true;
			} else {
				long daysSince = java.time.Duration.between(lastInteraction.get(), now).toDays();
				if (daysSince >= 30) {
					hasOverdue = true;
				} else if (daysSince >= 14) {
					hasAtRisk = true;
				} else {
					hasOnTrack = true;
				}
			}
		}

		assertThat(hasOnTrack).as("Should have at least one ON_TRACK employee").isTrue();
		assertThat(hasAtRisk).as("Should have at least one AT_RISK employee").isTrue();
		assertThat(hasOverdue).as("Should have at least one OVERDUE employee").isTrue();
	}
}
