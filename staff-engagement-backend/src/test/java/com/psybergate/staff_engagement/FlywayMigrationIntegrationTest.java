package com.psybergate.staff_engagement;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("local")
class FlywayMigrationIntegrationTest {

	@Autowired
	private Flyway flyway;

	@Test
	void baselineMigrationIsApplied() {
		MigrationInfo[] applied = flyway.info().applied();

		assertThat(applied).isNotEmpty();
		assertThat(applied[0].getVersion().getVersion()).isEqualTo("1");
		assertThat(applied[0].getDescription()).isEqualTo("baseline");
		assertThat(applied[0].getState()).isEqualTo(MigrationState.SUCCESS);
	}
}
