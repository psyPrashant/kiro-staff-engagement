package com.psybergate.acceptance.hooks;

import com.psybergate.acceptance.support.SqlScriptRunner;
import io.cucumber.java.Before;
import org.springframework.stereotype.Component;

@Component
public class GlobalTestDataHooks {

	private final SqlScriptRunner sqlScriptRunner;

	public GlobalTestDataHooks(SqlScriptRunner sqlScriptRunner) {
		this.sqlScriptRunner = sqlScriptRunner;
	}

	@Before(order = Integer.MIN_VALUE)
	public void cleanupTestData() {
		sqlScriptRunner.execute("fixtures/sql/global-cleanup.sql");
	}
}
