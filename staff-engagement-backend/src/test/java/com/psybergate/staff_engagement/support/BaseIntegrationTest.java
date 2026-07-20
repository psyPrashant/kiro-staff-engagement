package com.psybergate.staff_engagement.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("local")
public abstract class BaseIntegrationTest {
	// Shared configuration inherited by all integration tests.
	// Provides:
	// - Spring Boot context with a random port
	// - Testcontainers PostgreSQL via @ServiceConnection
	// - Flyway migrations applied automatically
	// - "local" profile activated
}
