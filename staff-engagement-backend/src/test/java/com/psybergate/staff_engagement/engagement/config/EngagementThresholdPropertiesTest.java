package com.psybergate.staff_engagement.engagement.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EngagementThresholdPropertiesTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void defaults_loadCorrectly() {
		var props = new EngagementThresholdProperties();
		assertThat(props.getOverdueDays()).isEqualTo(30);
		assertThat(props.getAtRiskDays()).isEqualTo(14);
	}

	@Test
	void atRiskDaysEqualToOverdueDays_throwsIllegalState() {
		var props = new EngagementThresholdProperties();
		props.setAtRiskDays(30);
		props.setOverdueDays(30);
		assertThatThrownBy(props::validateThresholdRelationship)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("at-risk threshold must be strictly less than overdue threshold");
	}

	@Test
	void atRiskDaysGreaterThanOverdueDays_throwsIllegalState() {
		var props = new EngagementThresholdProperties();
		props.setAtRiskDays(40);
		props.setOverdueDays(30);
		assertThatThrownBy(props::validateThresholdRelationship)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("at-risk threshold must be strictly less than overdue threshold");
	}

	@Test
	void overdueDaysZero_violatesMinConstraint() {
		var props = new EngagementThresholdProperties();
		props.setOverdueDays(0);
		Set<ConstraintViolation<EngagementThresholdProperties>> violations = validator.validate(props);
		assertThat(violations).isNotEmpty();
	}

	@Test
	void atRiskDaysNegative_violatesMinConstraint() {
		var props = new EngagementThresholdProperties();
		props.setAtRiskDays(-1);
		Set<ConstraintViolation<EngagementThresholdProperties>> violations = validator.validate(props);
		assertThat(violations).isNotEmpty();
	}

	@Test
	void overdueDaysExceeds365_violatesMaxConstraint() {
		var props = new EngagementThresholdProperties();
		props.setOverdueDays(366);
		Set<ConstraintViolation<EngagementThresholdProperties>> violations = validator.validate(props);
		assertThat(violations).isNotEmpty();
	}
}
