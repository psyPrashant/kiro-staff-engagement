package com.psybergate.acceptance.domain.scheduling;

import com.psybergate.acceptance.drivers.ui.pages.InteractionMatrixPage;
import com.psybergate.acceptance.drivers.ui.pages.ScheduleCalendarPage;
import com.psybergate.acceptance.drivers.ui.pages.ScheduleFormPage;
import com.psybergate.acceptance.world.TestWorld;
import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;

@Component
@ScenarioScope
public class SchedulingActor {

	private final ScheduleCalendarPage calendarPage;
	private final ScheduleFormPage formPage;
	private final InteractionMatrixPage matrixPage;
	private final TestWorld testWorld;

	public SchedulingActor(ScheduleCalendarPage calendarPage,
						   ScheduleFormPage formPage,
						   InteractionMatrixPage matrixPage,
						   TestWorld testWorld) {
		this.calendarPage = calendarPage;
		this.formPage = formPage;
		this.matrixPage = matrixPage;
		this.testWorld = testWorld;
	}

	public void scheduleNextFromMatrix(String employeeName) {
		matrixPage.clickScheduleNext(employeeName);
		testWorld.set("schedulingEmployeeName", employeeName);
	}

	public void fillScheduleForm(String date, String type) {
		formPage.setScheduledDate(date);
		formPage.selectInteractionType(type);
	}

	public void submitScheduleForm() {
		formPage.submit();
	}

	public void navigateToCalendar() {
		calendarPage.open();
	}

	public void completeEntry(String employeeName) {
		calendarPage.expandEntry(employeeName);
		calendarPage.clickComplete();
	}
}
