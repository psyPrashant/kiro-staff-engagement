package com.psybergate.acceptance.stepdefs.shell;

import com.psybergate.acceptance.domain.shell.DashboardActor;
import com.psybergate.acceptance.domain.shell.DashboardAssertions;
import com.psybergate.acceptance.world.TestWorld;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;

public class AppShellStepDefinitions {

	private final DashboardActor dashboardActor;
	private final DashboardAssertions dashboardAssertions;
	private final TestWorld testWorld;

	public AppShellStepDefinitions(DashboardActor dashboardActor, DashboardAssertions dashboardAssertions, TestWorld testWorld) {
		this.dashboardActor = dashboardActor;
		this.dashboardAssertions = dashboardAssertions;
		this.testWorld = testWorld;
	}

	@Then("the user should see the dashboard")
	public void theUserShouldSeeTheDashboard() {
		dashboardActor.waitForDashboard();
		dashboardAssertions.assertDashboardVisible();
	}

	@And("the navigation bar should be visible with links to all module areas")
	public void theNavigationBarShouldBeVisibleWithLinksToAllModuleAreas() {
		dashboardAssertions.assertNavBarVisibleWithAllLinks();
	}
}
