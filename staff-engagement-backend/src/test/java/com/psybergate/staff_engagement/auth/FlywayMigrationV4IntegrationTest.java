package com.psybergate.staff_engagement.auth;

import com.psybergate.staff_engagement.BaseIntegrationTest;
import com.psybergate.staff_engagement.user.User;
import com.psybergate.staff_engagement.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationV4IntegrationTest extends BaseIntegrationTest {

	private static final List<String> SEED_EMAILS = List.of(
			"alice.johnson@psybergate.com",
			"marcus.vanderberg@psybergate.com",
			"priya.naidoo@psybergate.com",
			"thabo.molefe@psybergate.com"
	);

	private static final String SEED_PASSWORD = "Password1";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private UserRepository userRepository;

	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);

	@Test
	void passwordHashColumnExistsOnUsersTable() {
		Integer columnCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM information_schema.columns " +
						"WHERE table_name = 'users' AND column_name = 'password_hash'",
				Integer.class
		);

		assertThat(columnCount).isEqualTo(1);
	}

	@Test
	void seedUsersHaveNonNullPasswordHash() {
		for (String email : SEED_EMAILS) {
			Optional<User> user = userRepository.findByEmail(email);

			assertThat(user)
					.as("User with email %s should exist", email)
					.isPresent();
			assertThat(user.get().getPasswordHash())
					.as("User %s should have a non-null password_hash", email)
					.isNotNull()
					.isNotBlank();
		}
	}

	@Test
	void seedUserHashesVerifyAgainstPassword1() {
		for (String email : SEED_EMAILS) {
			Optional<User> user = userRepository.findByEmail(email);

			assertThat(user).isPresent();

			boolean matches = passwordEncoder.matches(SEED_PASSWORD, user.get().getPasswordHash());

			assertThat(matches)
					.as("BCrypt hash for %s should verify against '%s'", email, SEED_PASSWORD)
					.isTrue();
		}
	}
}
