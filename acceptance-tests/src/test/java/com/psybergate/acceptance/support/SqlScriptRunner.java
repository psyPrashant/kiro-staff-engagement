package com.psybergate.acceptance.support;

import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Component
public class SqlScriptRunner {

	private static final int MAX_RETRIES = 3;
	private static final long RETRY_DELAY_MS = 200;

	private final DataSource dataSource;

	public SqlScriptRunner(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public void execute(String classpathResource) {
		String sql = readClasspathResource(classpathResource);
		executeWithRetry(sql);
	}

	private void executeWithRetry(String sql) {
		for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
			try (Connection connection = dataSource.getConnection();
			     Statement statement = connection.createStatement()) {
				statement.execute(sql);
				return;
			} catch (SQLException e) {
				if (isDeadlock(e) && attempt < MAX_RETRIES) {
					sleep(RETRY_DELAY_MS * attempt);
				} else {
					throw new RuntimeException(
						"SQL script execution failed after " + attempt + " attempt(s): " + e.getMessage(), e
					);
				}
			}
		}
	}

	private boolean isDeadlock(SQLException e) {
		String sqlState = e.getSQLState();
		// PostgreSQL deadlock detected: 40P01
		return "40P01".equals(sqlState) || "40001".equals(sqlState);
	}

	private void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted during deadlock retry", e);
		}
	}

	private String readClasspathResource(String resource) {
		try (InputStream is = getClass().getClassLoader().getResourceAsStream(resource)) {
			if (is == null) {
				throw new RuntimeException("Classpath resource not found: " + resource);
			}
			return new String(is.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException("Failed to read classpath resource: " + resource, e);
		}
	}
}
