package com.psybergate.acceptance.domain.employee360;

import com.psybergate.acceptance.drivers.ui.pages.Employee360Page;
import com.psybergate.acceptance.world.TestWorld;
import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;

@Component
@ScenarioScope
public class Employee360Actor {

	private final Employee360Page page;
	private final TestWorld testWorld;

	public Employee360Actor(Employee360Page page, TestWorld testWorld) {
		this.page = page;
		this.testWorld = testWorld;
	}

	public void navigateToEmployee360(Long employeeId) {
		page.open(employeeId);
		page.waitForProfileLoaded();
		testWorld.set("currentEmployeeId", employeeId);
	}
}
