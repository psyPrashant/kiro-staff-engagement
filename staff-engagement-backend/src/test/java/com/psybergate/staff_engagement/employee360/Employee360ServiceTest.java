package com.psybergate.staff_engagement.employee360;

import com.psybergate.staff_engagement.client.Company;
import com.psybergate.staff_engagement.client.Project;
import com.psybergate.staff_engagement.employee.Employee;
import com.psybergate.staff_engagement.employee.EmployeeRepository;
import com.psybergate.staff_engagement.interaction.Interaction;
import com.psybergate.staff_engagement.interaction.InteractionRepository;
import com.psybergate.staff_engagement.interaction.InteractionType;
import com.psybergate.staff_engagement.task.Task;
import com.psybergate.staff_engagement.task.TaskRepository;
import com.psybergate.staff_engagement.task.TaskStatus;
import com.psybergate.staff_engagement.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Employee360ServiceTest {

	@Mock
	private EmployeeRepository employeeRepository;

	@Mock
	private InteractionRepository interactionRepository;

	@Mock
	private TaskRepository taskRepository;

	@InjectMocks
	private Employee360Service employee360Service;

	@Test
	void getEmployee360_fullData_assemblesCompleteDtoCorrectly() {
		// Arrange
		Employee manager = new Employee();
		manager.setId(100L);
		manager.setName("Jane Manager");
		manager.setEmail("jane@example.com");
		manager.setJobTitle("Engineering Lead");
		manager.setCreatedAt(Instant.now());

		Employee employee = new Employee();
		employee.setId(1L);
		employee.setName("John Doe");
		employee.setEmail("john@example.com");
		employee.setJobTitle("Software Engineer");
		employee.setManager(manager);
		employee.setCreatedAt(Instant.now());

		User conductedBy = new User();
		conductedBy.setId(200L);
		conductedBy.setName("Jane Manager");
		conductedBy.setEmail("jane@example.com");
		conductedBy.setCreatedAt(Instant.now());

		User loggedBy = new User();
		loggedBy.setId(201L);
		loggedBy.setName("System Admin");
		loggedBy.setEmail("admin@example.com");
		loggedBy.setCreatedAt(Instant.now());

		Company company = new Company();
		company.setId(300L);
		company.setName("Acme Corp");
		company.setCreatedAt(Instant.now());

		Project project = new Project();
		project.setId(400L);
		project.setName("Alpha Platform");
		project.setCompany(company);
		project.setCreatedAt(Instant.now());

		Interaction interaction1 = new Interaction();
		interaction1.setId(10L);
		interaction1.setEmployee(employee);
		interaction1.setConductedBy(conductedBy);
		interaction1.setLoggedBy(loggedBy);
		interaction1.setProject(project);
		interaction1.setType(InteractionType.CHECK_IN);
		interaction1.setNotes("Discussed project progress");
		interaction1.setOccurredAt(Instant.parse("2024-12-15T10:00:00Z"));
		interaction1.setCreatedAt(Instant.now());

		Interaction interaction2 = new Interaction();
		interaction2.setId(11L);
		interaction2.setEmployee(employee);
		interaction2.setConductedBy(conductedBy);
		interaction2.setLoggedBy(loggedBy);
		interaction2.setProject(null);
		interaction2.setType(InteractionType.MENTORING);
		interaction2.setNotes("Career development discussion");
		interaction2.setOccurredAt(Instant.parse("2024-12-10T14:00:00Z"));
		interaction2.setCreatedAt(Instant.now());

		User assignedUser = new User();
		assignedUser.setId(202L);
		assignedUser.setName("John Doe");
		assignedUser.setEmail("john@example.com");
		assignedUser.setCreatedAt(Instant.now());

		Task task1 = new Task();
		task1.setId(20L);
		task1.setInteraction(interaction1);
		task1.setTitle("Update documentation");
		task1.setStatus(TaskStatus.OPEN);
		task1.setDueDate(LocalDate.of(2025, 1, 20));
		task1.setAssignedUser(assignedUser);
		task1.setCreatedAt(Instant.now());

		Task task2 = new Task();
		task2.setId(21L);
		task2.setInteraction(interaction2);
		task2.setTitle("Review code changes");
		task2.setStatus(TaskStatus.OPEN);
		task2.setDueDate(LocalDate.of(2025, 2, 1));
		task2.setAssignedUser(assignedUser);
		task2.setCreatedAt(Instant.now());

		when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
		when(interactionRepository.findByEmployeeIdOrderByOccurredAtDesc(1L))
			.thenReturn(List.of(interaction1, interaction2));
		when(taskRepository.findByInteractionIdInAndStatus(List.of(10L, 11L), TaskStatus.OPEN))
			.thenReturn(List.of(task1, task2));

		// Act
		Employee360Response response = employee360Service.getEmployee360(1L);

		// Assert - Profile
		assertThat(response.profile().id()).isEqualTo(1L);
		assertThat(response.profile().name()).isEqualTo("John Doe");
		assertThat(response.profile().email()).isEqualTo("john@example.com");
		assertThat(response.profile().jobTitle()).isEqualTo("Software Engineer");
		assertThat(response.profile().managerName()).isEqualTo("Jane Manager");

		// Assert - Interactions
		assertThat(response.interactions()).hasSize(2);

		InteractionDto firstInteraction = response.interactions().get(0);
		assertThat(firstInteraction.id()).isEqualTo(10L);
		assertThat(firstInteraction.type()).isEqualTo("CHECK_IN");
		assertThat(firstInteraction.occurredAt()).isEqualTo(Instant.parse("2024-12-15T10:00:00Z"));
		assertThat(firstInteraction.conductedByName()).isEqualTo("Jane Manager");
		assertThat(firstInteraction.notes()).isEqualTo("Discussed project progress");
		assertThat(firstInteraction.projectContext()).isNotNull();
		assertThat(firstInteraction.projectContext().projectName()).isEqualTo("Alpha Platform");
		assertThat(firstInteraction.projectContext().companyName()).isEqualTo("Acme Corp");

		InteractionDto secondInteraction = response.interactions().get(1);
		assertThat(secondInteraction.id()).isEqualTo(11L);
		assertThat(secondInteraction.type()).isEqualTo("MENTORING");
		assertThat(secondInteraction.projectContext()).isNull();

		// Assert - Open Tasks
		assertThat(response.openTasks()).hasSize(2);

		TaskDto firstTask = response.openTasks().get(0);
		assertThat(firstTask.id()).isEqualTo(20L);
		assertThat(firstTask.title()).isEqualTo("Update documentation");
		assertThat(firstTask.dueDate()).isEqualTo(LocalDate.of(2025, 1, 20));
		assertThat(firstTask.assignedUserName()).isEqualTo("John Doe");

		TaskDto secondTask = response.openTasks().get(1);
		assertThat(secondTask.id()).isEqualTo(21L);
		assertThat(secondTask.title()).isEqualTo("Review code changes");
		assertThat(secondTask.dueDate()).isEqualTo(LocalDate.of(2025, 2, 1));
		assertThat(secondTask.assignedUserName()).isEqualTo("John Doe");
	}

	@Test
	void getEmployee360_emptyInteractions_returnsEmptyLists() {
		// Arrange
		Employee employee = new Employee();
		employee.setId(2L);
		employee.setName("Alice Smith");
		employee.setEmail("alice@example.com");
		employee.setJobTitle("Designer");
		employee.setManager(null);
		employee.setCreatedAt(Instant.now());

		when(employeeRepository.findById(2L)).thenReturn(Optional.of(employee));
		when(interactionRepository.findByEmployeeIdOrderByOccurredAtDesc(2L))
			.thenReturn(List.of());

		// Act
		Employee360Response response = employee360Service.getEmployee360(2L);

		// Assert
		assertThat(response.interactions()).isEmpty();
		assertThat(response.openTasks()).isEmpty();
	}

	@Test
	void getEmployee360_nullManager_mapsToNullManagerName() {
		// Arrange
		Employee employee = new Employee();
		employee.setId(3L);
		employee.setName("Bob Jones");
		employee.setEmail("bob@example.com");
		employee.setJobTitle("Intern");
		employee.setManager(null);
		employee.setCreatedAt(Instant.now());

		when(employeeRepository.findById(3L)).thenReturn(Optional.of(employee));
		when(interactionRepository.findByEmployeeIdOrderByOccurredAtDesc(3L))
			.thenReturn(List.of());

		// Act
		Employee360Response response = employee360Service.getEmployee360(3L);

		// Assert
		assertThat(response.profile().managerName()).isNull();
	}

	@Test
	void getEmployee360_nullProject_mapsToNullProjectContext() {
		// Arrange
		Employee employee = new Employee();
		employee.setId(4L);
		employee.setName("Carol White");
		employee.setEmail("carol@example.com");
		employee.setJobTitle("Analyst");
		employee.setManager(null);
		employee.setCreatedAt(Instant.now());

		User conductedBy = new User();
		conductedBy.setId(210L);
		conductedBy.setName("Manager X");
		conductedBy.setEmail("managerx@example.com");
		conductedBy.setCreatedAt(Instant.now());

		User loggedBy = new User();
		loggedBy.setId(211L);
		loggedBy.setName("Logger Y");
		loggedBy.setEmail("loggery@example.com");
		loggedBy.setCreatedAt(Instant.now());

		Interaction interaction = new Interaction();
		interaction.setId(30L);
		interaction.setEmployee(employee);
		interaction.setConductedBy(conductedBy);
		interaction.setLoggedBy(loggedBy);
		interaction.setProject(null);
		interaction.setType(InteractionType.CATCH_UP);
		interaction.setNotes("General catch up");
		interaction.setOccurredAt(Instant.parse("2024-11-01T09:00:00Z"));
		interaction.setCreatedAt(Instant.now());

		when(employeeRepository.findById(4L)).thenReturn(Optional.of(employee));
		when(interactionRepository.findByEmployeeIdOrderByOccurredAtDesc(4L))
			.thenReturn(List.of(interaction));
		when(taskRepository.findByInteractionIdInAndStatus(List.of(30L), TaskStatus.OPEN))
			.thenReturn(List.of());

		// Act
		Employee360Response response = employee360Service.getEmployee360(4L);

		// Assert
		assertThat(response.interactions()).hasSize(1);
		assertThat(response.interactions().get(0).projectContext()).isNull();
	}

	@Test
	void getEmployee360_nullDueDate_mapsToNullDueDateInDto() {
		// Arrange
		Employee employee = new Employee();
		employee.setId(5L);
		employee.setName("Dave Green");
		employee.setEmail("dave@example.com");
		employee.setJobTitle("Developer");
		employee.setManager(null);
		employee.setCreatedAt(Instant.now());

		User conductedBy = new User();
		conductedBy.setId(220L);
		conductedBy.setName("Lead Dev");
		conductedBy.setEmail("lead@example.com");
		conductedBy.setCreatedAt(Instant.now());

		User loggedBy = new User();
		loggedBy.setId(221L);
		loggedBy.setName("Admin");
		loggedBy.setEmail("admin2@example.com");
		loggedBy.setCreatedAt(Instant.now());

		Interaction interaction = new Interaction();
		interaction.setId(40L);
		interaction.setEmployee(employee);
		interaction.setConductedBy(conductedBy);
		interaction.setLoggedBy(loggedBy);
		interaction.setProject(null);
		interaction.setType(InteractionType.OTHER);
		interaction.setNotes("Ad hoc discussion");
		interaction.setOccurredAt(Instant.parse("2024-10-20T16:00:00Z"));
		interaction.setCreatedAt(Instant.now());

		User assignedUser = new User();
		assignedUser.setId(222L);
		assignedUser.setName("Dave Green");
		assignedUser.setEmail("dave@example.com");
		assignedUser.setCreatedAt(Instant.now());

		Task task = new Task();
		task.setId(50L);
		task.setInteraction(interaction);
		task.setTitle("Follow up on discussion");
		task.setStatus(TaskStatus.OPEN);
		task.setDueDate(null);
		task.setAssignedUser(assignedUser);
		task.setCreatedAt(Instant.now());

		when(employeeRepository.findById(5L)).thenReturn(Optional.of(employee));
		when(interactionRepository.findByEmployeeIdOrderByOccurredAtDesc(5L))
			.thenReturn(List.of(interaction));
		when(taskRepository.findByInteractionIdInAndStatus(List.of(40L), TaskStatus.OPEN))
			.thenReturn(List.of(task));

		// Act
		Employee360Response response = employee360Service.getEmployee360(5L);

		// Assert
		assertThat(response.openTasks()).hasSize(1);
		assertThat(response.openTasks().get(0).dueDate()).isNull();
		assertThat(response.openTasks().get(0).title()).isEqualTo("Follow up on discussion");
		assertThat(response.openTasks().get(0).assignedUserName()).isEqualTo("Dave Green");
	}

	@Test
	void getEmployee360_employeeNotFound_throwsEmployee360NotFoundException() {
		// Arrange
		when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

		// Act & Assert
		assertThatThrownBy(() -> employee360Service.getEmployee360(999L))
			.isInstanceOf(Employee360NotFoundException.class)
			.hasMessageContaining("Employee not found with id: 999");
	}
}
