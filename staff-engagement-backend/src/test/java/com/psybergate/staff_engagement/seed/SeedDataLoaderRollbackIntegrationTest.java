package com.psybergate.staff_engagement.seed;

import com.psybergate.staff_engagement.BaseIntegrationTest;
import com.psybergate.staff_engagement.employee.EmployeeRepository;
import com.psybergate.staff_engagement.interaction.InteractionRepository;
import com.psybergate.staff_engagement.scheduling.ScheduledInteractionRepository;
import com.psybergate.staff_engagement.task.TaskRepository;
import com.psybergate.staff_engagement.user.User;
import com.psybergate.staff_engagement.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test verifying that the SeedDataLoader's {@code @Transactional}
 * annotation correctly rolls back ALL changes when an uncaught constraint
 * violation occurs during insertion.
 * <p>
 * Strategy:
 * <ol>
 *   <li>The seed data is already loaded at context startup (the "local" profile is active).</li>
 *   <li>Delete all existing data to start fresh.</li>
 *   <li>Pre-insert a user with email "marcus.vanderberg@psybergate.com" (a seed user email)
 *       causing a unique constraint violation when the loader attempts to create Marcus.</li>
 *   <li>Invoke the seed loader's {@code run()} method — the transaction should roll back.</li>
 *   <li>Verify that no partial data persists from the failed re-seed attempt.</li>
 * </ol>
 * <p>
 * Validates: Requirements 1.4, 7.6
 */
@DirtiesContext
class SeedDataLoaderRollbackIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private SeedDataLoader seedDataLoader;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EmployeeRepository employeeRepository;

	@Autowired
	private InteractionRepository interactionRepository;

	@Autowired
	private TaskRepository taskRepository;

	@Autowired
	private ScheduledInteractionRepository scheduledInteractionRepository;

	@Test
	void seedDataLoader_rollsBackOnConstraintViolation() {
		// 1. Delete all data to start fresh
		scheduledInteractionRepository.deleteAll();
		taskRepository.deleteAll();
		interactionRepository.deleteAll();
		employeeRepository.deleteAll();
		userRepository.deleteAll();

		// 2. Pre-insert a conflicting user with "marcus.vanderberg@psybergate.com"
		// This email is used by the seed loader for one of its users.
		// The seed loader first creates Alice, then Marcus — the unique email
		// constraint will cause an uncaught DataIntegrityViolationException.
		User conflictingUser = new User();
		conflictingUser.setName("Conflict User");
		conflictingUser.setEmail("marcus.vanderberg@psybergate.com");
		conflictingUser.setPasswordHash("dummy-hash");
		userRepository.saveAndFlush(conflictingUser);

		// 3. Invoke the seed loader — should fail and roll back
		try {
			seedDataLoader.run(new ApplicationArguments() {
				@Override
				public String[] getSourceArgs() { return new String[0]; }
				@Override
				public java.util.Set<String> getOptionNames() { return java.util.Set.of(); }
				@Override
				public boolean containsOption(String name) { return false; }
				@Override
				public java.util.List<String> getOptionValues(String name) { return null; }
				@Override
				public java.util.List<String> getNonOptionArgs() { return java.util.List.of(); }
			});
		} catch (Exception e) {
			// Expected: DataIntegrityViolationException from the unique constraint violation
		}

		// 4. Verify no partial data persists — only the pre-inserted conflicting user
		// should remain. The seed loader's transactional boundary ensures all-or-nothing.
		assertThat(userRepository.count())
				.as("Only the pre-inserted conflict user should exist (transaction rolled back)")
				.isEqualTo(1);
		assertThat(employeeRepository.count())
				.as("No employees should be created (transaction rolled back)")
				.isEqualTo(0);
		assertThat(interactionRepository.count())
				.as("No interactions should be created (transaction rolled back)")
				.isEqualTo(0);
		assertThat(taskRepository.count())
				.as("No tasks should be created (transaction rolled back)")
				.isEqualTo(0);
		assertThat(scheduledInteractionRepository.count())
				.as("No scheduled interactions should be created (transaction rolled back)")
				.isEqualTo(0);
	}
}
