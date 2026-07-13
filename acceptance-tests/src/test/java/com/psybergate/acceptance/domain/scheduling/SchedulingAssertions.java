package com.psybergate.acceptance.domain.scheduling;

import com.psybergate.acceptance.drivers.ui.pages.ScheduleCalendarPage;
import com.psybergate.acceptance.drivers.ui.pages.ScheduleFormPage;
import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

@Component
@ScenarioScope
public class SchedulingAssertions {

	private final ScheduleCalendarPage calendarPage;
	private final ScheduleFormPage formPage;

	public SchedulingAssertions(ScheduleCalendarPage calendarPage, ScheduleFormPage formPage) {
		this.calendarPage = calendarPage;
		this.formPage = formPage;
	}

	public void assertEntryVisible(String employeeName) {
		assertThat(calendarPage.isEntryVisible(employeeName))
			.as("Expected schedule entry for '%s' to be visible", employeeName)
			.isTrue();
	}

	public void assertEntryNotVisible(String employeeName) {
		assertThat(calendarPage.isEntryVisible(employeeName))
			.as("Expected schedule entry for '%s' to NOT be visible", employeeName)
			.isFalse();
	}

	public void assertOverdueIndicator(String employeeName) {
		assertThat(calendarPage.hasOverdueIndicator(employeeName))
			.as("Expected overdue indicator for '%s'", employeeName)
			.isTrue();
	}

	public void assertDateValidationError() {
		assertThat(formPage.isDateValidationErrorVisible())
			.as("Expected date validation error to be visible")
			.isTrue();
	}

	public void assertSubmitDisabled() {
		assertThat(formPage.isSubmitEnabled())
			.as("Expected submit button to be disabled")
			.isFalse();
	}
}
