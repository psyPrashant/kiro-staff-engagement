package com.psybergate.staff_engagement.seed;

import com.psybergate.staff_engagement.client.domain.Company;
import com.psybergate.staff_engagement.client.domain.CompanyRepository;
import com.psybergate.staff_engagement.client.domain.Project;
import com.psybergate.staff_engagement.client.domain.ProjectRepository;
import com.psybergate.staff_engagement.employee.domain.Employee;
import com.psybergate.staff_engagement.employee.domain.EmployeeRepository;
import com.psybergate.staff_engagement.interaction.domain.Interaction;
import com.psybergate.staff_engagement.interaction.domain.InteractionRepository;
import com.psybergate.staff_engagement.interaction.domain.InteractionType;
import com.psybergate.staff_engagement.scheduling.domain.CompletionStatus;
import com.psybergate.staff_engagement.scheduling.domain.ScheduledInteraction;
import com.psybergate.staff_engagement.scheduling.domain.ScheduledInteractionRepository;
import com.psybergate.staff_engagement.task.domain.Task;
import com.psybergate.staff_engagement.task.domain.TaskRepository;
import com.psybergate.staff_engagement.task.domain.TaskStatus;
import com.psybergate.staff_engagement.user.domain.User;
import com.psybergate.staff_engagement.user.domain.UserRepository;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

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
		// ─── 1. USERS (People Managers / Engagement Leads who log in) ───
		User alice = createUser("Alice Johnson", "alice.johnson@psybergate.com");
		User marcus = createUser("Marcus van der Berg", "marcus.vanderberg@psybergate.com");
		User priya = createUser("Priya Naidoo", "priya.naidoo@psybergate.com");
		User thabo = createUser("Thabo Molefe", "thabo.molefe@psybergate.com");

		// ─── 2. COMPANIES (Client organisations) ───
		Company nedbank = createCompany("Nedbank");
		Company discovery = createCompany("Discovery Health");
		Company standardBank = createCompany("Standard Bank");
		Company woolworths = createCompany("Woolworths Financial Services");

		// ─── 3. PROJECTS (Client engagements) ───
		Project nedbankDigital = createProject("Digital Banking Platform", nedbank);
		Project nedbankMobile = createProject("Mobile App Revamp", nedbank);
		Project discoveryVitality = createProject("Vitality Rewards Engine", discovery);
		Project discoveryOnboard = createProject("Member Onboarding Portal", discovery);
		Project sbPayments = createProject("Cross-Border Payments API", standardBank);
		Project woolworthsLoyalty = createProject("WRewards Loyalty System", woolworths);

		// ─── 4. EMPLOYEES (Staff being managed) ───

		// Senior / Team leads (no manager set — top of the hierarchy)
		Employee sipho = createEmployee("Sipho Dlamini", "sipho.dlamini@psybergate.com", "Engineering Manager", null);
		Employee fatima = createEmployee("Fatima Patel", "fatima.patel@psybergate.com", "Delivery Lead", null);

		// Mid-level (report to leads)
		Employee liam = createEmployee("Liam O'Connor", "liam.oconnor@psybergate.com", "Senior Software Engineer", sipho);
		Employee naledi = createEmployee("Naledi Khumalo", "naledi.khumalo@psybergate.com", "Senior Software Engineer", sipho);
		Employee ravi = createEmployee("Ravi Govender", "ravi.govender@psybergate.com", "Full Stack Developer", fatima);
		Employee zanele = createEmployee("Zanele Mthembu", "zanele.mthembu@psybergate.com", "Business Analyst", fatima);
		Employee james = createEmployee("James Botha", "james.botha@psybergate.com", "DevOps Engineer", sipho);

		// Junior (report to mid-level)
		Employee amara = createEmployee("Amara Okafor", "amara.okafor@psybergate.com", "Graduate Developer", liam);
		Employee dylan = createEmployee("Dylan Ferreira", "dylan.ferreira@psybergate.com", "Junior Developer", liam);
		Employee lerato = createEmployee("Lerato Mokoena", "lerato.mokoena@psybergate.com", "Junior QA Engineer", naledi);
		Employee chen = createEmployee("Chen Wei", "chen.wei@psybergate.com", "Intern - Software Development", ravi);
		Employee sarah = createEmployee("Sarah van Wyk", "sarah.vanwyk@psybergate.com", "Junior UX Designer", zanele);
		Employee tumelo = createEmployee("Tumelo Mabaso", "tumelo.mabaso@psybergate.com", "Graduate Developer", naledi);

		// ─── 5. INTERACTIONS ───
		// Goal: create a mix of engagement statuses for the dashboard
		// ON_TRACK  = last interaction < 14 days ago
		// AT_RISK   = last interaction 14–29 days ago
		// OVERDUE   = last interaction ≥ 30 days ago or none

		// --- Sipho (ON_TRACK) — interacted 3 days ago ---
		Interaction siphoCheckIn = createInteraction(sipho, alice, alice, nedbankDigital,
				InteractionType.CHECK_IN, 3,
				"Discussed architecture decisions for the new microservices migration. Sipho is confident about the timeline and has escalated a dependency on the DevOps team.");
		createInteraction(sipho, alice, alice, nedbankDigital,
				InteractionType.MENTORING, 20,
				"Walked through leadership principles relevant to cross-team coordination. Agreed on a reading list for Q3.");

		// --- Fatima (ON_TRACK) — interacted 7 days ago ---
		Interaction fatimaCheckIn = createInteraction(fatima, marcus, marcus, discoveryVitality,
				InteractionType.CHECK_IN, 7,
				"Reviewed delivery cadence with the Discovery team. Sprint velocity has stabilised after last month's restructure.");
		createInteraction(fatima, marcus, marcus, null,
				InteractionType.CATCH_UP, 35,
				"Informal catch-up about workload. She mentioned possible burnout signs — follow up needed.");

		// --- Liam (ON_TRACK) — interacted 5 days ago ---
		Interaction liamMentoring = createInteraction(liam, alice, alice, nedbankMobile,
				InteractionType.MENTORING, 5,
				"Paired on a system design exercise for the payment gateway refactor. Liam suggested an event-driven approach which was well thought out.");
		createInteraction(liam, priya, priya, nedbankMobile,
				InteractionType.CHECK_IN, 18,
				"Mid-sprint check-in. No blockers. Liam took on a stretch goal to improve test coverage on the auth module.");

		// --- Naledi (AT_RISK) — last interaction 16 days ago ---
		Interaction nalediCheckIn = createInteraction(naledi, priya, priya, sbPayments,
				InteractionType.CHECK_IN, 16,
				"Naledi expressed frustration with unclear requirements from the Standard Bank product owner. We agreed to set up a direct channel.");
		createInteraction(naledi, alice, alice, sbPayments,
				InteractionType.OTHER, 45,
				"Ad-hoc discussion after Naledi flagged a production incident. She handled it well under pressure.");

		// --- Ravi (AT_RISK) — last interaction 21 days ago ---
		Interaction raviCatchUp = createInteraction(ravi, marcus, marcus, discoveryOnboard,
				InteractionType.CATCH_UP, 21,
				"Ravi is enjoying the Discovery onboarding project but feels stretched thin across two squads. We'll revisit allocation next sprint.");
		createInteraction(ravi, thabo, thabo, discoveryOnboard,
				InteractionType.MENTORING, 50,
				"Discussed career progression pathways. Ravi is keen to move toward a tech lead role within the next 12 months.");

		// --- Zanele (ON_TRACK) — interacted 2 days ago ---
		Interaction zaneleMentoring = createInteraction(zanele, thabo, thabo, woolworthsLoyalty,
				InteractionType.MENTORING, 2,
				"Coached Zanele on stakeholder mapping techniques for the Woolworths loyalty initiative. She prepared an excellent context diagram.");
		createInteraction(zanele, thabo, thabo, woolworthsLoyalty,
				InteractionType.CHECK_IN, 14,
				"Discussed upcoming requirements workshop. Zanele will facilitate — good growth opportunity.");

		// --- James (OVERDUE) — last interaction 40 days ago ---
		Interaction jamesCheckIn = createInteraction(james, marcus, marcus, nedbankDigital,
				InteractionType.CHECK_IN, 40,
				"Checked in on the CI/CD pipeline overhaul. James is making progress but working in isolation. Needs more visibility into business priorities.");

		// --- Amara (ON_TRACK) — interacted 1 day ago ---
		Interaction amaraCheckIn = createInteraction(amara, alice, alice, nedbankMobile,
				InteractionType.CHECK_IN, 1,
				"Onboarding follow-up. Amara has completed her first feature branch and submitted a PR. Pairing with Liam is going well.");
		createInteraction(amara, alice, priya, nedbankMobile,
				InteractionType.MENTORING, 8,
				"Covered Git branching strategy and code review etiquette. Amara asked great questions about rebase vs merge.");

		// --- Dylan (AT_RISK) — last interaction 18 days ago ---
		Interaction dylanCheckIn = createInteraction(dylan, priya, priya, nedbankMobile,
				InteractionType.CHECK_IN, 18,
				"Dylan is settling in but seems hesitant to ask for help. Encouraged him to attend mob programming sessions.");

		// --- Lerato (OVERDUE) — last interaction 33 days ago ---
		Interaction leratoCheckIn = createInteraction(lerato, priya, priya, sbPayments,
				InteractionType.CHECK_IN, 33,
				"Lerato is doing well on the payments testing but hasn't had a formal interaction in a while. She requested more frequent check-ins.");

		// --- Chen (ON_TRACK) — interacted 4 days ago ---
		Interaction chenMentoring = createInteraction(chen, thabo, thabo, discoveryOnboard,
				InteractionType.MENTORING, 4,
				"Reviewed Chen's intern project proposal. Solid approach to building a dashboard component. Will present to team next Friday.");

		// --- Sarah (OVERDUE) — no recent interactions, last was 60 days ago ---
		Interaction sarahOther = createInteraction(sarah, marcus, marcus, woolworthsLoyalty,
				InteractionType.OTHER, 60,
				"Informal chat after a design critique session. Sarah's confidence has grown significantly since joining.");

		// --- Tumelo (AT_RISK) — last interaction 25 days ago ---
		Interaction tumeloCheckIn = createInteraction(tumelo, alice, alice, sbPayments,
				InteractionType.CHECK_IN, 25,
				"Tumelo completed his first production deployment successfully. Needs to focus on writing better unit tests.");

		// ─── 6. TASKS ───
		// Mix of OPEN and DONE, with realistic due dates

		// Tasks from Sipho's check-in
		createTask("Finalise microservices migration RFC", "Draft the RFC document outlining the proposed service boundaries, data ownership, and migration timeline for the Nedbank platform.",
				TaskStatus.OPEN, LocalDate.now().plusDays(10), siphoCheckIn, alice, sipho);
		createTask("Schedule dependency review with DevOps", "Set up a 30-min session with James to clarify the infra requirements for the new services.",
				TaskStatus.DONE, LocalDate.now().minusDays(1), siphoCheckIn, alice, sipho);

		// Tasks from Fatima's check-in
		createTask("Submit delivery health report for Discovery", "Compile sprint velocity, defect rate, and team happiness scores for the monthly client review.",
				TaskStatus.OPEN, LocalDate.now().plusDays(5), fatimaCheckIn, marcus, fatima);

		// Tasks from Liam's mentoring
		createTask("Complete payment gateway design document", "Document the event-driven architecture proposal including sequence diagrams and failure handling.",
				TaskStatus.OPEN, LocalDate.now().plusDays(14), liamMentoring, alice, liam);
		createTask("Increase auth module test coverage to 85%", "Write additional unit and integration tests for the authentication module edge cases.",
				TaskStatus.DONE, LocalDate.now().minusDays(3), liamMentoring, priya, liam);

		// Tasks from Naledi's check-in
		createTask("Set up direct channel with Standard Bank PO", "Create a Slack Connect channel and schedule a weekly 15-min alignment call.",
				TaskStatus.OPEN, LocalDate.now().plusDays(3), nalediCheckIn, priya, naledi);
		createTask("Document API contract discrepancies", "List the gaps between the spec and actual implementation for the payments endpoint.",
				TaskStatus.OPEN, LocalDate.now().plusDays(7), nalediCheckIn, priya, naledi);

		// Tasks from Ravi's catch-up
		createTask("Propose revised squad allocation", "Draft a recommendation for reducing Ravi's involvement in the secondary squad by 50%.",
				TaskStatus.OPEN, LocalDate.now().plusDays(7), raviCatchUp, marcus, ravi);

		// Tasks from Zanele's mentoring
		createTask("Prepare requirements workshop agenda", "Create the facilitation plan including icebreaker, persona review, and story mapping exercise.",
				TaskStatus.DONE, LocalDate.now().minusDays(2), zaneleMentoring, thabo, zanele);

		// Tasks from James' check-in
		createTask("Present CI/CD progress to engineering leads", "Prepare a 10-minute demo of the new pipeline including deployment metrics and rollback capability.",
				TaskStatus.OPEN, LocalDate.now().plusDays(12), jamesCheckIn, marcus, james);
		createTask("Shadow a sprint planning session", "Attend the Nedbank squad's next planning session to understand business priority setting.",
				TaskStatus.OPEN, LocalDate.now().plusDays(5), jamesCheckIn, marcus, james);

		// Tasks from Amara's check-in
		createTask("Complete PR review checklist training", "Work through the team's PR review guidelines and submit a self-assessment.",
				TaskStatus.DONE, LocalDate.now().minusDays(1), amaraCheckIn, alice, amara);

		// Tasks from Dylan's check-in
		createTask("Attend at least two mob programming sessions", "Join the team's scheduled mob sessions this sprint and share learnings.",
				TaskStatus.OPEN, LocalDate.now().plusDays(10), dylanCheckIn, priya, dylan);

		// Tasks from Lerato's check-in
		createTask("Set up bi-weekly check-in cadence", "Schedule recurring 30-min fortnightly check-ins with Priya starting next week.",
				TaskStatus.OPEN, LocalDate.now().plusDays(4), leratoCheckIn, priya, lerato);

		// Tasks from Chen's mentoring
		createTask("Present intern project proposal to team", "Prepare slides and a short demo of the dashboard component prototype for Friday's showcase.",
				TaskStatus.OPEN, LocalDate.now().plusDays(6), chenMentoring, thabo, chen);

		// Tasks from Tumelo's check-in
		createTask("Complete unit testing module on learning platform", "Finish the 'Effective Unit Testing in Java' course and share certificate.",
				TaskStatus.OPEN, LocalDate.now().plusDays(14), tumeloCheckIn, alice, tumelo);

		// Overdue tasks (due date in the past, still OPEN)
		createTask("Submit updated CV for client allocation", "Update CV with latest project experience for the Standard Bank RFP.",
				TaskStatus.OPEN, LocalDate.now().minusDays(5), sarahOther, marcus, sarah);

		// ─── 7. SCHEDULED INTERACTIONS ───
		// Mix of PENDING (future), COMPLETED (past), and CANCELLED (past)

		List<Employee> allEmployees = List.of(sipho, fatima, liam, naledi, ravi, zanele, james, amara, dylan, lerato, chen, sarah, tumelo);

		// This week (PENDING) — high-visibility items for demo
		createScheduledInteraction(alice, naledi, InteractionType.CHECK_IN, CompletionStatus.PENDING,
				LocalDate.now(), "Catch up on Standard Bank requirements alignment — urgent.");
		createScheduledInteraction(marcus, james, InteractionType.MENTORING, CompletionStatus.PENDING,
				LocalDate.now(), "Pair on presenting CI/CD metrics to leadership.");
		createScheduledInteraction(priya, dylan, InteractionType.MENTORING, CompletionStatus.PENDING,
				LocalDate.now().plusDays(1), "Mob programming intro session with Dylan.");
		createScheduledInteraction(thabo, chen, InteractionType.CHECK_IN, CompletionStatus.PENDING,
				LocalDate.now().plusDays(2), "Mid-week check-in before Friday presentation.");
		createScheduledInteraction(alice, amara, InteractionType.CHECK_IN, CompletionStatus.PENDING,
				LocalDate.now().plusDays(3), "Onboarding progress review — week 4 milestone.");
		createScheduledInteraction(marcus, ravi, InteractionType.CHECK_IN, CompletionStatus.PENDING,
				LocalDate.now().plusDays(4), "Revisit squad allocation after sprint planning.");

		// Upcoming (PENDING) — shows in "Next Scheduled" on dashboard & employee 360
		createScheduledInteraction(alice, sipho, InteractionType.CHECK_IN, CompletionStatus.PENDING,
				LocalDate.now().plusDays(4), "Fortnightly check-in to review migration progress and team morale.");
		createScheduledInteraction(marcus, fatima, InteractionType.CHECK_IN, CompletionStatus.PENDING,
				LocalDate.now().plusDays(6), "Monthly delivery review and workload assessment.");
		createScheduledInteraction(alice, liam, InteractionType.MENTORING, CompletionStatus.PENDING,
				LocalDate.now().plusDays(3), "System design follow-up — review Liam's event-sourcing proposal.");
		createScheduledInteraction(priya, naledi, InteractionType.CHECK_IN, CompletionStatus.PENDING,
				LocalDate.now().plusDays(2), "Urgent follow-up on requirements frustration and PO alignment.");
		createScheduledInteraction(marcus, ravi, InteractionType.CATCH_UP, CompletionStatus.PENDING,
				LocalDate.now().plusDays(8), "Revisit squad allocation decision and check energy levels.");
		createScheduledInteraction(thabo, zanele, InteractionType.CHECK_IN, CompletionStatus.PENDING,
				LocalDate.now().plusDays(12), "Post-workshop debrief and next steps.");
		createScheduledInteraction(marcus, james, InteractionType.CHECK_IN, CompletionStatus.PENDING,
				LocalDate.now().plusDays(1), "Overdue check-in — discuss visibility gaps and business context.");
		createScheduledInteraction(alice, amara, InteractionType.MENTORING, CompletionStatus.PENDING,
				LocalDate.now().plusDays(7), "Onboarding milestone review at 30-day mark.");
		createScheduledInteraction(priya, dylan, InteractionType.CHECK_IN, CompletionStatus.PENDING,
				LocalDate.now().plusDays(5), "Follow-up on confidence building and mob programming attendance.");
		createScheduledInteraction(priya, lerato, InteractionType.CHECK_IN, CompletionStatus.PENDING,
				LocalDate.now().plusDays(3), "First session in new bi-weekly cadence.");
		createScheduledInteraction(thabo, chen, InteractionType.MENTORING, CompletionStatus.PENDING,
				LocalDate.now().plusDays(9), "Review intern project after team presentation.");
		createScheduledInteraction(marcus, sarah, InteractionType.CHECK_IN, CompletionStatus.PENDING,
				LocalDate.now().plusDays(2), "Overdue engagement — reconnect and discuss current challenges.");
		createScheduledInteraction(alice, tumelo, InteractionType.MENTORING, CompletionStatus.PENDING,
				LocalDate.now().plusDays(11), "Unit testing coaching session after he completes the online module.");

		// Completed (past)
		createScheduledInteraction(alice, sipho, InteractionType.CHECK_IN, CompletionStatus.COMPLETED,
				LocalDate.now().minusDays(11), "Regular fortnightly check-in — went well, covered architecture decisions.");
		createScheduledInteraction(marcus, fatima, InteractionType.CHECK_IN, CompletionStatus.COMPLETED,
				LocalDate.now().minusDays(28), "Monthly review completed. Discussed team restructure impact.");
		createScheduledInteraction(alice, liam, InteractionType.CHECK_IN, CompletionStatus.COMPLETED,
				LocalDate.now().minusDays(18), "Mid-sprint touchpoint. No concerns raised.");
		createScheduledInteraction(thabo, zanele, InteractionType.MENTORING, CompletionStatus.COMPLETED,
				LocalDate.now().minusDays(14), "Stakeholder mapping session went very well.");
		createScheduledInteraction(thabo, chen, InteractionType.MENTORING, CompletionStatus.COMPLETED,
				LocalDate.now().minusDays(4), "Reviewed project proposal. Chen is on track.");

		// Cancelled
		createScheduledInteraction(priya, naledi, InteractionType.CATCH_UP, CompletionStatus.CANCELLED,
				LocalDate.now().minusDays(10), "Cancelled due to client escalation — rescheduled as check-in.");
		createScheduledInteraction(marcus, james, InteractionType.CHECK_IN, CompletionStatus.CANCELLED,
				LocalDate.now().minusDays(20), "James was on leave. Needs to be rescheduled.");
	}

	// ─── Helper methods ───

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
			Project project, InteractionType type, int daysAgo, String notes) {
		Interaction interaction = new Interaction();
		interaction.setEmployee(employee);
		interaction.setConductedBy(conductedBy);
		interaction.setLoggedBy(loggedBy);
		interaction.setProject(project);
		interaction.setType(type);
		interaction.setNotes(notes);
		interaction.setOccurredAt(Instant.now().minus(daysAgo, ChronoUnit.DAYS));
		return interactionRepository.save(interaction);
	}

	private void createTask(String title, String description, TaskStatus status,
			LocalDate dueDate, Interaction interaction, User assignedUser, Employee employee) {
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

	private void createScheduledInteraction(User scheduledBy, Employee employee,
			InteractionType type, CompletionStatus status, LocalDate date, String notes) {
		ScheduledInteraction scheduled = new ScheduledInteraction();
		scheduled.setEmployee(employee);
		scheduled.setScheduledBy(scheduledBy);
		scheduled.setScheduledDate(date);
		scheduled.setInteractionType(type);
		scheduled.setNotes(notes);
		scheduled.setCompletionStatus(status);
		scheduledInteractionRepository.save(scheduled);
	}
}
