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
import com.psybergate.staff_engagement.scheduling.CompletionStatus;
import com.psybergate.staff_engagement.scheduling.ScheduledInteraction;
import com.psybergate.staff_engagement.scheduling.ScheduledInteractionRepository;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile({"local", "dev"})
@RequiredArgsConstructor
@Slf4j
public class SeedDataLoader implements ApplicationRunner {

	private static final String SEED_USER_EMAIL = "alice.johnson@psybergate.com";
	private static final String SEED_PASSWORD = "Password1";

	private final UserRepository userRepository;
	private final CompanyRepository companyRepository;
	private final EmployeeRepository employeeRepository;
	private final ProjectRepository projectRepository;
	private final InteractionRepository interactionRepository;
	private final TaskRepository taskRepository;
	private final ScheduledInteractionRepository scheduledInteractionRepository;
	private final PasswordEncoder passwordEncoder;

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

		// 8. New Users
		List<User> newUsers = createNewUsers();

		// 9. New Employees
		List<Employee> existingEmployees = List.of(empJane, empJohn, empSarah, empMike, empLisa);
		List<Employee> newEmployees = createNewEmployees(existingEmployees);

		// 10. Interactions for new employees
		List<User> allUsers = List.of(alice, bob, carol, newUsers.get(0), newUsers.get(1));
		List<Project> allProjects = List.of(projectAlpha, projectBeta, projectGamma);
		List<Interaction> newInteractions = createInteractionsForNewEmployees(newEmployees, allUsers, allProjects);

		// 11. Tasks for employees
		createTasksForEmployees(newEmployees, newInteractions, allUsers);

		// 12. Scheduled interactions
		List<Employee> allEmployees = new ArrayList<>(existingEmployees);
		allEmployees.addAll(newEmployees);
		createScheduledInteractions(allUsers, allEmployees);
	}

	private List<User> createNewUsers() {
		User dave = createUser("Dave Martinez", "dave.martinez@psybergate.com");
		User eve = createUser("Eve Thompson", "eve.thompson@psybergate.com");
		return List.of(dave, eve);
	}

	private List<Employee> createNewEmployees(List<Employee> existingEmployees) {
		String[] firstNames = {
				"Alex", "Jordan", "Taylor", "Morgan", "Casey",
				"Riley", "Quinn", "Avery", "Cameron", "Dakota",
				"Skyler", "Reese", "Finley", "Harper", "Emerson",
				"Parker", "Rowan", "Sage", "Blair", "Drew"
		};
		String[] lastNames = {
				"Anderson", "Brooks", "Campbell", "Davis", "Evans",
				"Foster", "Garcia", "Harris", "Ingram", "Jackson",
				"Kim", "Lopez", "Mitchell", "Nelson", "Ortiz",
				"Patel", "Quinn", "Rivera", "Santos", "Turner"
		};
		String[] jobTitles = {
				"Software Engineer", "Senior Developer", "Team Lead",
				"Product Manager", "UX Designer", "QA Engineer",
				"DevOps Engineer", "Business Analyst", "Data Analyst", "Scrum Master"
		};

		List<Employee> newEmployees = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			String name = firstNames[i] + " " + lastNames[i];
			String email = firstNames[i].toLowerCase() + "." + lastNames[i].toLowerCase() + "@company.com";
			String jobTitle = jobTitles[i % jobTitles.length];
			Employee manager = (i < 10) ? existingEmployees.get(i % existingEmployees.size()) : null;

			try {
				Employee employee = new Employee();
				employee.setName(name);
				employee.setEmail(email);
				employee.setJobTitle(jobTitle);
				employee.setManager(manager);
				Employee saved = employeeRepository.saveAndFlush(employee);
				newEmployees.add(saved);
			} catch (DataIntegrityViolationException e) {
				log.warn("Skipping duplicate employee with email: {}", email);
			}
		}
		return newEmployees;
	}

	private List<Interaction> createInteractionsForNewEmployees(List<Employee> newEmployees, List<User> allUsers, List<Project> allProjects) {
		String[] topics = {
				"progress review", "goal setting", "skill development",
				"team collaboration", "project update", "career growth",
				"performance feedback", "knowledge sharing", "process improvement",
				"onboarding support"
		};

		List<Interaction> allInteractions = new ArrayList<>();
		for (int empIdx = 0; empIdx < newEmployees.size(); empIdx++) {
			Employee employee = newEmployees.get(empIdx);
			for (int i = 0; i < 20; i++) {
				InteractionType type = InteractionType.values()[i % 4];
				User conductedBy = allUsers.get(i % 5);
				User loggedBy = allUsers.get((i + 2) % 5);
				Project project = (i <= 7) ? allProjects.get(i % 3) : null;
				Instant occurredAt = Instant.now().minus((long) i * 18, ChronoUnit.DAYS);
				String notes = String.format("%s discussion covering key topics and action items",
						topics[i % topics.length]);

				Interaction interaction = new Interaction();
				interaction.setEmployee(employee);
				interaction.setConductedBy(conductedBy);
				interaction.setLoggedBy(loggedBy);
				interaction.setProject(project);
				interaction.setType(type);
				interaction.setNotes(notes);
				interaction.setOccurredAt(occurredAt);
				Interaction saved = interactionRepository.save(interaction);
				allInteractions.add(saved);
			}
		}
		return allInteractions;
	}

	private void createTasksForEmployees(List<Employee> employees, List<Interaction> interactions, List<User> allUsers) {
		TaskStatus[] statuses = {TaskStatus.OPEN, TaskStatus.OPEN, TaskStatus.OPEN, TaskStatus.DONE, TaskStatus.DONE};
		int[] dueDayOffsets = {7, 14, -7, -30, -60};
		String[] actionVerbs = {"Review", "Complete", "Prepare", "Update", "Finalize"};
		String[] topics = {"sprint goals", "code review training", "roadmap draft", "documentation", "team assessment"};

		for (int empIdx = 0; empIdx < 5; empIdx++) {
			Employee employee = employees.get(empIdx);
			for (int taskIdx = 0; taskIdx < 5; taskIdx++) {
				String title = String.format("%s %s",
						actionVerbs[taskIdx], topics[taskIdx]);
				String description = String.format(
						"detailed action items for %s",
						topics[taskIdx]);
				TaskStatus status = statuses[taskIdx];
				LocalDate dueDate = LocalDate.now().plusDays(dueDayOffsets[taskIdx]);
				Interaction interaction = interactions.get(empIdx * 20 + taskIdx);
				User assignedUser = allUsers.get(taskIdx % 5);

				Task task = new Task();
				task.setTitle(title);
				task.setDescription(description);
				task.setStatus(status);
				task.setDueDate(dueDate);
				task.setInteraction(interaction);
				task.setAssignedUser(assignedUser);
				task.setEmployee(employee);
				taskRepository.save(task);
			}
		}
	}

	private void createScheduledInteractions(List<User> allUsers, List<Employee> allEmployees) {
		InteractionType[] types = {InteractionType.CHECK_IN, InteractionType.MENTORING, InteractionType.CATCH_UP};
		CompletionStatus[] statuses = {CompletionStatus.PENDING, CompletionStatus.COMPLETED, CompletionStatus.CANCELLED};

		for (int userIdx = 0; userIdx < allUsers.size(); userIdx++) {
			User user = allUsers.get(userIdx);
			for (int schedIdx = 0; schedIdx < 3; schedIdx++) {
				InteractionType type = types[schedIdx % types.length];
				CompletionStatus status = statuses[schedIdx % statuses.length];
				Employee employee = allEmployees.get((userIdx * 3 + schedIdx) % allEmployees.size());

				LocalDate date;
				if (status == CompletionStatus.PENDING) {
					// Future date: +7d to +30d
					date = LocalDate.now().plusDays(7 + (userIdx * 3 + schedIdx) % 24);
				} else {
					// Past date: -1d to -90d
					date = LocalDate.now().minusDays(1 + (userIdx * 3 + schedIdx) % 90);
				}

				String notes = String.format("Scheduled interaction %d-%d: %s with %s",
						userIdx, schedIdx, type.name(), employee.getName());

				createScheduledInteraction(user, employee, type, status, date, notes);
			}
		}
	}

	private ScheduledInteraction createScheduledInteraction(User user, Employee employee,
			InteractionType type, CompletionStatus status, LocalDate date, String notes) {
		ScheduledInteraction scheduled = new ScheduledInteraction();
		scheduled.setEmployee(employee);
		scheduled.setScheduledBy(user);
		scheduled.setScheduledDate(date);
		scheduled.setInteractionType(type);
		scheduled.setNotes(notes);
		scheduled.setCompletionStatus(status);
		return scheduledInteractionRepository.save(scheduled);
	}

	private User createUser(String name, String email) {
		User user = new User();
		user.setName(name);
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode(SEED_PASSWORD));
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
