package com.psybergate.acceptance.domain.shell;

import com.psybergate.acceptance.drivers.ui.pages.AppShellPage;
import com.psybergate.acceptance.drivers.ui.pages.DashboardPage;
import com.psybergate.acceptance.world.TestWorld;
import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

@Component
@ScenarioScope
public class DashboardAssertions {

	private final DashboardPage dashboardPage;
	private final AppShellPage appShellPage;
	private final TestWorld testWorld;

	public DashboardAssertions(DashboardPage dashboardPage, AppShellPage appShellPage, TestWorld testWorld) {
		this.dashboardPage = dashboardPage;
		this.appShellPage = appShellPage;
		this.testWorld = testWorld;
	}

	public void assertDashboardVisible() {
		assertThat(dashboardPage.isDashboardVisible())
			.as("Expected the dashboard to be visible")
			.isTrue();
	}

	public void assertNavBarVisibleWithAllLinks() {
		assertThat(appShellPage.isNavBarVisible())
			.as("Expected the navigation bar to be visible")
			.isTrue();

		assertThat(appShellPage.isNavLinkVisible("nav-link-user"))
			.as("Expected nav link 'user' to be visible")
			.isTrue();

		assertThat(appShellPage.isNavLinkVisible("nav-link-employee"))
			.as("Expected nav link 'employee' to be visible")
			.isTrue();

		assertThat(appShellPage.isNavLinkVisible("nav-link-client"))
			.as("Expected nav link 'client' to be visible")
			.isTrue();

		assertThat(appShellPage.isNavLinkVisible("nav-link-interaction"))
			.as("Expected nav link 'interaction' to be visible")
			.isTrue();

		assertThat(appShellPage.isNavLinkVisible("nav-link-task"))
			.as("Expected nav link 'task' to be visible")
			.isTrue();
	}
}
