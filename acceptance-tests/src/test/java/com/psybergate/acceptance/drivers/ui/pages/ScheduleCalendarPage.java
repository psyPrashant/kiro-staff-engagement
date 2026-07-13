package com.psybergate.acceptance.drivers.ui.pages;

import com.microsoft.playwright.Page;
import com.psybergate.acceptance.config.EnvironmentConfig;
import org.springframework.stereotype.Component;

import io.cucumber.spring.ScenarioScope;

@Component
@ScenarioScope
public class ScheduleCalendarPage extends BasePage {

	public ScheduleCalendarPage(Page page, EnvironmentConfig env) {
		super(page, env);
	}

	public void open() {
		navigateTo("/schedule");
	}

	public boolean isEntryVisible(String employeeName) {
		return isVisible("[data-testid='schedule-entry'] :text('" + employeeName + "')");
	}

	public boolean hasOverdueIndicator(String employeeName) {
		return isVisible("[data-testid='schedule-entry'].overdue :text('" + employeeName + "')");
	}

	public boolean isEmptyStateVisible() {
		return isVisible("[data-testid='empty-schedule']");
	}

	public void expandEntry(String employeeName) {
		click("[data-testid='schedule-entry'] :text('" + employeeName + "')");
	}

	public void clickComplete() {
		click("[data-testid='complete-btn']");
	}

	public void clickCancel() {
		click("[data-testid='cancel-btn']");
	}

	public boolean isLoadingVisible() {
		return isVisible("[data-testid='loading-indicator']");
	}
}
