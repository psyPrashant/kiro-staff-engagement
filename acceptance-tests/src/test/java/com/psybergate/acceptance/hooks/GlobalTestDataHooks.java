package com.psybergate.acceptance.hooks;

import com.psybergate.acceptance.support.SqlScriptRunner;
import io.cucumber.java.Before;

public class GlobalTestDataHooks {

	private final SqlScriptRunner sqlScriptRunner;

	public GlobalTestDataHooks(SqlScriptRunner sqlScriptRunner) {
		this.sqlScriptRunner = sqlScriptRunner;
	}

	/**
	 * Full truncate for isolation. Skipped for {@code @backend-seed} scenarios, which instead
	 * rely on the backend's startup seed data (see {@code BackendSeedHooks}).
	 */
	@Before(value = "not @backend-seed", order = Integer.MIN_VALUE)
	public void cleanupTestData() {
		sqlScriptRunner.execute("fixtures/sql/global-cleanup.sql");
	}
}
