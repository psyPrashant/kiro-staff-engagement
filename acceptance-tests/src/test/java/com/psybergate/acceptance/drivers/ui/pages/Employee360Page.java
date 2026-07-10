package com.psybergate.acceptance.drivers.ui.pages;

import com.microsoft.playwright.Page;
import com.psybergate.acceptance.config.EnvironmentConfig;
import org.springframework.stereotype.Component;

import io.cucumber.spring.ScenarioScope;

@Component
@ScenarioScope
public class Employee360Page extends BasePage {

	public Employee360Page(Page page, EnvironmentConfig env) {
		super(page, env);
	}

	public void open(Long employeeId) {
		navigateTo("/employee/" + employeeId);
	}

	public boolean isProfileSummaryVisible() {
		return isVisible("[data-testid='profile-summary']");
	}

	public String getEmployeeName() {
		return textContent("[data-testid='employee-name']");
	}

	public boolean isInteractionHistoryVisible() {
		return isVisible("[data-testid='interaction-history']");
	}

	public boolean isOpenTasksVisible() {
		return isVisible("[data-testid='open-tasks']");
	}

	public boolean isEmptyInteractionsMessageVisible() {
		return isVisible("[data-testid='empty-interactions']");
	}

	public boolean isEmptyTasksMessageVisible() {
		return isVisible("[data-testid='empty-tasks']");
	}

	public boolean hasOverdueTaskStyling() {
		return isVisible("[data-testid='task-row'].overdue");
	}

	public void waitForProfileLoaded() {
		waitForVisible("[data-testid='profile-summary']");
	}
}
