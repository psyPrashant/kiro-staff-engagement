package com.psybergate.acceptance.domain.employee360;

import com.psybergate.acceptance.drivers.ui.pages.Employee360Page;
import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

@Component
@ScenarioScope
public class Employee360Assertions {

	private final Employee360Page page;

	public Employee360Assertions(Employee360Page page) {
		this.page = page;
	}

	public void assertProfileSummaryIsVisible() {
		assertThat(page.isProfileSummaryVisible())
			.as("Expected the profile summary to be visible")
			.isTrue();
	}

	public void assertInteractionHistoryIsVisible() {
		assertThat(page.isInteractionHistoryVisible())
			.as("Expected the interaction history to be visible")
			.isTrue();
	}

	public void assertOpenTasksAreVisible() {
		assertThat(page.isOpenTasksVisible())
			.as("Expected open tasks to be visible")
			.isTrue();
	}

	public void assertEmptyInteractionsMessageShown() {
		assertThat(page.isEmptyInteractionsMessageVisible())
			.as("Expected empty interactions message to be shown")
			.isTrue();
	}

	public void assertEmptyTasksMessageShown() {
		assertThat(page.isEmptyTasksMessageVisible())
			.as("Expected empty tasks message to be shown")
			.isTrue();
	}

	public void assertOverdueTasksAreDistinguished() {
		assertThat(page.hasOverdueTaskStyling())
			.as("Expected overdue tasks to be visually distinguished")
			.isTrue();
	}
}
