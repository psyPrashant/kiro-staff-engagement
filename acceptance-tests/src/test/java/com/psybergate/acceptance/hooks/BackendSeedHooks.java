package com.psybergate.acceptance.hooks;

import com.psybergate.acceptance.drivers.api.AuthApiDriver;
import com.psybergate.acceptance.support.SqlScriptRunner;
import io.cucumber.java.Before;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Setup for {@code @backend-seed} scenarios (the API-only suites that verify the backend's own
 * startup seed data — {@code @seed-data} and {@code @next-scheduled}).
 *
 * <p>Unlike the UI scenarios, these must NOT wipe the seed tables. Instead we only clear the
 * transactional {@code scheduled_interactions} table (so each scenario starts with no pending
 * interactions) and then authenticate as the seeded user so the {@code /api/**} endpoints are
 * reachable.
 */
public class BackendSeedHooks {

	// The user created by the backend's SeedDataLoader (local/dev profile).
	private static final String SEED_USER_EMAIL = "alice.johnson@psybergate.com";
	private static final String SEED_USER_PASSWORD = "Password1";

	private final SqlScriptRunner sqlScriptRunner;
	private final AuthApiDriver authApiDriver;

	public BackendSeedHooks(SqlScriptRunner sqlScriptRunner, AuthApiDriver authApiDriver) {
		this.sqlScriptRunner = sqlScriptRunner;
		this.authApiDriver = authApiDriver;
	}

	@Before(value = "@backend-seed", order = Integer.MIN_VALUE)
	public void resetScheduledInteractionsAndLogIn() {
		sqlScriptRunner.execute("fixtures/sql/scheduled-interactions-cleanup.sql");

		int status = authApiDriver.login(SEED_USER_EMAIL, SEED_USER_PASSWORD).statusCode();
		assertThat(status)
			.as("Login as seeded user '%s' should succeed (is the backend running with seed data?)",
				SEED_USER_EMAIL)
			.isEqualTo(200);
	}
}
