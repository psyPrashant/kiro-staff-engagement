package com.psybergate.acceptance.drivers.ui.pages;

import com.microsoft.playwright.Page;
import com.psybergate.acceptance.config.EnvironmentConfig;
import org.springframework.stereotype.Component;

import io.cucumber.spring.ScenarioScope;

@Component
@ScenarioScope
public class ScheduleFormPage extends BasePage {

	public ScheduleFormPage(Page page, EnvironmentConfig env) {
		super(page, env);
	}

	public void open(Long employeeId) {
		navigateTo("/schedule/new?employeeId=" + employeeId);
	}

	public String getEmployeeDisplay() {
		return textContent("[data-testid='employee-display']");
	}

	public void setScheduledDate(String date) {
		fill("[data-testid='scheduled-date-input']", date);
	}

	public void selectInteractionType(String type) {
		page.locator("[data-testid='interaction-type-select']").selectOption(type);
	}

	public void setNotes(String notes) {
		fill("[data-testid='notes-input']", notes);
	}

	public void submit() {
		click("[data-testid='submit-btn']");
	}

	public boolean isSubmitEnabled() {
		return !page.locator("[data-testid='submit-btn']").isDisabled();
	}

	public boolean isDateValidationErrorVisible() {
		return isVisible("[data-testid='date-validation-error']");
	}

	public boolean isApiErrorVisible() {
		return isVisible("[data-testid='api-error']");
	}

	public boolean isEmployeeErrorVisible() {
		return isVisible("[data-testid='employee-error']");
	}
}
