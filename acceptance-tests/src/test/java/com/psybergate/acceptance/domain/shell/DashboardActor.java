package com.psybergate.acceptance.domain.shell;

import com.psybergate.acceptance.drivers.ui.pages.DashboardPage;
import com.psybergate.acceptance.world.TestWorld;
import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;

@Component
@ScenarioScope
public class DashboardActor {

	private final DashboardPage dashboardPage;
	private final TestWorld testWorld;

	public DashboardActor(DashboardPage dashboardPage, TestWorld testWorld) {
		this.dashboardPage = dashboardPage;
		this.testWorld = testWorld;
	}

	public void waitForDashboard() {
		dashboardPage.waitForDashboard();
	}
}
