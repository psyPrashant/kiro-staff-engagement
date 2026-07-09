package com.psybergate.staff_engagement.seed;

import com.psybergate.staff_engagement.client.Company;
import com.psybergate.staff_engagement.client.CompanyRepository;
import com.psybergate.staff_engagement.client.Project;
import com.psybergate.staff_engagement.client.ProjectRepository;
import com.psybergate.staff_engagement.employee.Employee;
import com.psybergate.staff_engagement.employee.EmployeeRepository;
import com.psybergate.staff_engagement.interaction.Interaction;
import com.psybergate.staff_engagement.interaction.InteractionRepository;
import com.psybergate.staff_engagement.interaction.InteractionType;
import com.psybergate.staff_engagement.task.Task;
import com.psybergate.staff_engagement.task.TaskRepository;
import com.psybergate.staff_engagement.task.TaskStatus;
import com.psybergate.staff_engagement.user.User;
import com.psybergate.staff_engagement.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;

@Component
@Profile({"local", "dev"})
@RequiredArgsConstructor
@Slf4j
public class SeedDataLoader implements ApplicationRunner {

	private static final String SEED_USER_EMAIL = "alice.johnson@psybergate.com";

	private final UserRepository userRepository;
	private final CompanyRepository companyRepository;
	private final EmployeeRepository employeeRepository;
	private final ProjectRepository projectRepository;
	private final InteractionRepository interactionRepository;
	private final TaskRepository taskRepository;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (seedDataAlreadyPresent()) {
			log.info("Seed data already present — skipping insertion.");
			return;
		}
		insertSeedData();
		log.info("Seed data loaded successfully.");
	}

	private boolean seedDataAlreadyPresent() {
		return userRepository.findByEmail(SEED_USER_EMAIL).isPresent();
	}

	private void insertSeedData() {
		// 1. Users (no FK dependencies)
		User alice = createUser("Alice Johnson", "alice.johnson@psybergate.com");
		User bob = createUser("Bob Smith", "bob.smith@psybergate.com");
		User carol = createUser("Carol Williams", "carol.williams@psybergate.com");

		// 2. Companies (no FK dependencies)
		Company acme = createCompany("Acme Corp");
		Company globex = createCompany("Globex Inc");

		// 3. Employees without managers
		Employee empJane = createEmployee("Jane Doe", "jane.doe@acme.com", "Software Engineer", null);
		Employee empJohn = createEmployee("John Wick", "john.wick@acme.com", "Team Lead", null);
		Employee empSarah = createEmployee("Sarah Connor", "sarah.connor@globex.com", "Product Manager", null);

		// 4. Employees with managers (self-referential FK satisfied by step 3)
		Employee empMike = createEmployee("Mike Ross", "mike.ross@acme.com", "Junior Developer", empJohn);
		Employee empLisa = createEmployee("Lisa Chen", "lisa.chen@globex.com", "UX Designer", empSarah);

		// 5. Projects (FK → Companies)
		Project projectAlpha = createProject("Project Alpha", acme);
		Project projectBeta = createProject("Project Beta", acme);
		Project projectGamma = createProject("Project Gamma", globex);

		// 6. Interactions (FK → Employees, Users, optionally Projects)
		Interaction checkIn = createInteraction(empJane, alice, alice, null,
				InteractionType.CHECK_IN, "Weekly check-in with Jane to discuss progress.");
		Interaction mentoring = createInteraction(empMike, bob, bob, projectAlpha,
				InteractionType.MENTORING, "Mentoring session on code review best practices.");
		Interaction catchUp = createInteraction(empSarah, carol, carol, null,
				InteractionType.CATCH_UP, "Catch-up meeting to review Q3 goals.");
		Interaction other = createInteraction(empLisa, alice, bob, null,
				InteractionType.OTHER, "Ad-hoc discussion about design system improvements.");

		// 7. Tasks (FK → Interactions, optionally Users)
		createTask("Follow up on sprint goals", "Review sprint backlog and prioritize items",
				TaskStatus.OPEN, null, checkIn, alice);
		createTask("Complete code review training", "Finish the online module on review techniques",
				TaskStatus.DONE, null, mentoring, bob);
		createTask("Prepare Q4 roadmap draft", "Draft the initial roadmap for stakeholder review",
				TaskStatus.OPEN, LocalDate.now().plusDays(14), catchUp, carol);
	}

	private User createUser(String name, String email) {
		User user = new User();
		user.setName(name);
		user.setEmail(email);
		return userRepository.save(user);
	}

	private Company createCompany(String name) {
		Company company = new Company();
		company.setName(name);
		return companyRepository.save(company);
	}

	private Employee createEmployee(String name, String email, String jobTitle, Employee manager) {
		Employee employee = new Employee();
		employee.setName(name);
		employee.setEmail(email);
		employee.setJobTitle(jobTitle);
		employee.setManager(manager);
		return employeeRepository.save(employee);
	}

	private Project createProject(String name, Company company) {
		Project project = new Project();
		project.setName(name);
		project.setCompany(company);
		return projectRepository.save(project);
	}

	private Interaction createInteraction(Employee employee, User conductedBy, User loggedBy,
			Project project, InteractionType type, String notes) {
		Interaction interaction = new Interaction();
		interaction.setEmployee(employee);
		interaction.setConductedBy(conductedBy);
		interaction.setLoggedBy(loggedBy);
		interaction.setProject(project);
		interaction.setType(type);
		interaction.setNotes(notes);
		interaction.setOccurredAt(Instant.now());
		return interactionRepository.save(interaction);
	}

	private Task createTask(String title, String description, TaskStatus status,
			LocalDate dueDate, Interaction interaction, User assignedUser) {
		Task task = new Task();
		task.setTitle(title);
		task.setDescription(description);
		task.setStatus(status);
		task.setDueDate(dueDate);
		task.setInteraction(interaction);
		task.setAssignedUser(assignedUser);
		return taskRepository.save(task);
	}
}
