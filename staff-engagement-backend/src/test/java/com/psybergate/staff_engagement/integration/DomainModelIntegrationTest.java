package com.psybergate.staff_engagement.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.psybergate.staff_engagement.client.domain.Company;
import com.psybergate.staff_engagement.client.domain.CompanyRepository;
import com.psybergate.staff_engagement.client.domain.Project;
import com.psybergate.staff_engagement.client.domain.ProjectRepository;
import com.psybergate.staff_engagement.employee.domain.Employee;
import com.psybergate.staff_engagement.employee.domain.EmployeeRepository;
import com.psybergate.staff_engagement.interaction.domain.Interaction;
import com.psybergate.staff_engagement.interaction.domain.InteractionRepository;
import com.psybergate.staff_engagement.interaction.domain.InteractionType;
import com.psybergate.staff_engagement.support.TestcontainersConfiguration;
import com.psybergate.staff_engagement.task.domain.Task;
import com.psybergate.staff_engagement.task.domain.TaskRepository;
import com.psybergate.staff_engagement.task.domain.TaskStatus;
import com.psybergate.staff_engagement.user.domain.User;
import com.psybergate.staff_engagement.user.domain.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("local")
@Transactional
class DomainModelIntegrationTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EmployeeRepository employeeRepository;

	@Autowired
	private CompanyRepository companyRepository;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private InteractionRepository interactionRepository;

	@Autowired
	private TaskRepository taskRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void allRepositoriesAreInjectableAndCanSaveAndFindById() {
		User user = new User();
		user.setName("Test User");
		user.setEmail("user-repo-test@example.com");
		User savedUser = userRepository.save(user);
		entityManager.flush();
		assertThat(userRepository.findById(savedUser.getId())).isPresent();

		Employee employee = new Employee();
		employee.setName("Test Employee");
		employee.setEmail("employee-repo-test@example.com");
		employee.setJobTitle("Developer");
		Employee savedEmployee = employeeRepository.save(employee);
		entityManager.flush();
		assertThat(employeeRepository.findById(savedEmployee.getId())).isPresent();

		Company company = new Company();
		company.setName("Test Company");
		Company savedCompany = companyRepository.save(company);
		entityManager.flush();
		assertThat(companyRepository.findById(savedCompany.getId())).isPresent();

		Project project = new Project();
		project.setName("Test Project");
		project.setCompany(savedCompany);
		Project savedProject = projectRepository.save(project);
		entityManager.flush();
		assertThat(projectRepository.findById(savedProject.getId())).isPresent();

		Interaction interaction = new Interaction();
		interaction.setEmployee(savedEmployee);
		interaction.setConductedBy(savedUser);
		interaction.setLoggedBy(savedUser);
		interaction.setType(InteractionType.CHECK_IN);
		interaction.setNotes("Test notes");
		interaction.setOccurredAt(Instant.now());
		Interaction savedInteraction = interactionRepository.save(interaction);
		entityManager.flush();
		assertThat(interactionRepository.findById(savedInteraction.getId())).isPresent();

		Task task = new Task();
		task.setTitle("Test Task");
		task.setDescription("Test Description");
		Task savedTask = taskRepository.save(task);
		entityManager.flush();
		assertThat(taskRepository.findById(savedTask.getId())).isPresent();
	}

	@Test
	void fullEntityGraphPersistenceAndRetrievalWithAssociationsResolved() {
		User conductor = new User();
		conductor.setName("Conductor");
		conductor.setEmail("conductor@example.com");
		userRepository.save(conductor);

		User logger = new User();
		logger.setName("Logger");
		logger.setEmail("logger@example.com");
		userRepository.save(logger);

		User assignee = new User();
		assignee.setName("Assignee");
		assignee.setEmail("assignee@example.com");
		userRepository.save(assignee);

		Employee manager = new Employee();
		manager.setName("Manager");
		manager.setEmail("manager@example.com");
		manager.setJobTitle("Team Lead");
		employeeRepository.save(manager);

		Employee employee = new Employee();
		employee.setName("Employee");
		employee.setEmail("employee@example.com");
		employee.setJobTitle("Developer");
		employee.setManager(manager);
		employeeRepository.save(employee);

		Company company = new Company();
		company.setName("Acme Corp");
		companyRepository.save(company);

		Project project = new Project();
		project.setName("Project Alpha");
		project.setCompany(company);
		projectRepository.save(project);

		Interaction interaction = new Interaction();
		interaction.setEmployee(employee);
		interaction.setConductedBy(conductor);
		interaction.setLoggedBy(logger);
		interaction.setProject(project);
		interaction.setType(InteractionType.MENTORING);
		interaction.setNotes("Mentoring session about career growth");
		interaction.setOccurredAt(Instant.now());
		interactionRepository.save(interaction);

		Task task = new Task();
		task.setTitle("Follow up on mentoring goals");
		task.setDescription("Check progress on career development plan");
		task.setInteraction(interaction);
		task.setAssignedUser(assignee);
		task.setDueDate(LocalDate.now().plusDays(7));
		taskRepository.save(task);

		entityManager.flush();
		entityManager.clear();

		// Reload and verify associations
		Interaction reloadedInteraction = interactionRepository.findById(interaction.getId()).orElseThrow();
		assertThat(reloadedInteraction.getEmployee().getId()).isEqualTo(employee.getId());
		assertThat(reloadedInteraction.getEmployee().getName()).isEqualTo("Employee");
		assertThat(reloadedInteraction.getConductedBy().getId()).isEqualTo(conductor.getId());
		assertThat(reloadedInteraction.getLoggedBy().getId()).isEqualTo(logger.getId());
		assertThat(reloadedInteraction.getProject()).isNotNull();
		assertThat(reloadedInteraction.getProject().getId()).isEqualTo(project.getId());
		assertThat(reloadedInteraction.getProject().getCompany().getId()).isEqualTo(company.getId());

		Task reloadedTask = taskRepository.findById(task.getId()).orElseThrow();
		assertThat(reloadedTask.getInteraction()).isNotNull();
		assertThat(reloadedTask.getInteraction().getId()).isEqualTo(interaction.getId());
		assertThat(reloadedTask.getAssignedUser()).isNotNull();
		assertThat(reloadedTask.getAssignedUser().getId()).isEqualTo(assignee.getId());
	}

	@Test
	void nullableProjectOnInteraction() {
		User user = new User();
		user.setName("User");
		user.setEmail("user-nullable-project@example.com");
		userRepository.save(user);

		Employee employee = new Employee();
		employee.setName("Employee");
		employee.setEmail("emp-nullable-project@example.com");
		employeeRepository.save(employee);

		Interaction interaction = new Interaction();
		interaction.setEmployee(employee);
		interaction.setConductedBy(user);
		interaction.setLoggedBy(user);
		interaction.setProject(null);
		interaction.setType(InteractionType.CATCH_UP);
		interaction.setNotes("Quick catch up without project context");
		interaction.setOccurredAt(Instant.now());
		interactionRepository.save(interaction);

		entityManager.flush();
		entityManager.clear();

		Interaction reloaded = interactionRepository.findById(interaction.getId()).orElseThrow();
		assertThat(reloaded.getProject()).isNull();
	}

	@Test
	void nullableInteractionAndAssignedUserOnTask() {
		Task task = new Task();
		task.setTitle("Standalone task");
		task.setInteraction(null);
		task.setAssignedUser(null);
		taskRepository.save(task);

		entityManager.flush();
		entityManager.clear();

		Task reloaded = taskRepository.findById(task.getId()).orElseThrow();
		assertThat(reloaded.getInteraction()).isNull();
		assertThat(reloaded.getAssignedUser()).isNull();
	}

	@Test
	void enumMappingCorrectnessForAllInteractionTypeValues() {
		User user = new User();
		user.setName("Enum Test User");
		user.setEmail("enum-interaction@example.com");
		userRepository.save(user);

		Employee employee = new Employee();
		employee.setName("Enum Test Employee");
		employee.setEmail("enum-emp@example.com");
		employeeRepository.save(employee);

		for (InteractionType type : InteractionType.values()) {
			Interaction interaction = new Interaction();
			interaction.setEmployee(employee);
			interaction.setConductedBy(user);
			interaction.setLoggedBy(user);
			interaction.setType(type);
			interaction.setNotes("Testing type: " + type.name());
			interaction.setOccurredAt(Instant.now());
			interactionRepository.save(interaction);

			entityManager.flush();
			entityManager.clear();

			Interaction reloaded = interactionRepository.findById(interaction.getId()).orElseThrow();
			assertThat(reloaded.getType()).isEqualTo(type);
		}
	}

	@Test
	void enumMappingCorrectnessForAllTaskStatusValues() {
		for (TaskStatus status : TaskStatus.values()) {
			Task task = new Task();
			task.setTitle("Task with status: " + status.name());
			task.setStatus(status);
			taskRepository.save(task);

			entityManager.flush();
			entityManager.clear();

			Task reloaded = taskRepository.findById(task.getId()).orElseThrow();
			assertThat(reloaded.getStatus()).isEqualTo(status);
		}
	}

	@Test
	void selfReferencingEmployeeManagerRelationship() {
		Employee topManager = new Employee();
		topManager.setName("Top Manager");
		topManager.setEmail("top-manager@example.com");
		topManager.setJobTitle("Director");
		topManager.setManager(null);
		employeeRepository.save(topManager);

		Employee midManager = new Employee();
		midManager.setName("Mid Manager");
		midManager.setEmail("mid-manager@example.com");
		midManager.setJobTitle("Team Lead");
		midManager.setManager(topManager);
		employeeRepository.save(midManager);

		Employee developer = new Employee();
		developer.setName("Developer");
		developer.setEmail("developer@example.com");
		developer.setJobTitle("Software Engineer");
		developer.setManager(midManager);
		employeeRepository.save(developer);

		entityManager.flush();
		entityManager.clear();

		Employee reloadedDeveloper = employeeRepository.findById(developer.getId()).orElseThrow();
		assertThat(reloadedDeveloper.getManager()).isNotNull();
		assertThat(reloadedDeveloper.getManager().getId()).isEqualTo(midManager.getId());
		assertThat(reloadedDeveloper.getManager().getName()).isEqualTo("Mid Manager");

		Employee reloadedMidManager = employeeRepository.findById(midManager.getId()).orElseThrow();
		assertThat(reloadedMidManager.getManager()).isNotNull();
		assertThat(reloadedMidManager.getManager().getId()).isEqualTo(topManager.getId());

		Employee reloadedTopManager = employeeRepository.findById(topManager.getId()).orElseThrow();
		assertThat(reloadedTopManager.getManager()).isNull();
	}
}
