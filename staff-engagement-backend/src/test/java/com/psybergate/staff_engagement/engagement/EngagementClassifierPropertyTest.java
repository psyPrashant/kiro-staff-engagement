package com.psybergate.staff_engagement.engagement;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for EngagementClassifier.
 * Uses jqwik to verify universal properties across randomized inputs.
 */
class EngagementClassifierPropertyTest {

	// Feature: interaction-matrix-followups, Property 4: Engagement classification is deterministic and correct
	// **Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.6, 7.8**

	@Property(tries = 200)
	void classificationIsDeterministicAndCorrect(
			@ForAll @IntRange(min = 0, max = 365) int recency,
			@ForAll @IntRange(min = 1, max = 364) int atRiskThreshold,
			@ForAll @IntRange(min = 2, max = 365) int overdueThreshold) {

		Assume.that(atRiskThreshold < overdueThreshold);

		EngagementStatus status = EngagementClassifier.classify(recency, atRiskThreshold, overdueThreshold);

		if (recency >= overdueThreshold) {
			assertThat(status).isEqualTo(EngagementStatus.OVERDUE);
		} else if (recency >= atRiskThreshold) {
			assertThat(status).isEqualTo(EngagementStatus.AT_RISK);
		} else {
			assertThat(status).isEqualTo(EngagementStatus.ON_TRACK);
		}
	}

	@Property(tries = 200)
	void nullRecencyAlwaysClassifiesAsOverdue(
			@ForAll @IntRange(min = 1, max = 364) int atRiskThreshold,
			@ForAll @IntRange(min = 2, max = 365) int overdueThreshold) {

		Assume.that(atRiskThreshold < overdueThreshold);

		EngagementStatus status = EngagementClassifier.classify(null, atRiskThreshold, overdueThreshold);

		assertThat(status).isEqualTo(EngagementStatus.OVERDUE);
	}

	// Feature: interaction-matrix-followups, Property 5: Follow-up flag is derived from engagement status
	// **Validates: Requirements 3.1, 3.2, 3.3**

	@Property(tries = 200)
	void followUpFlagIsDerivedFromStatus(
			@ForAll @IntRange(min = 0, max = 365) int recency,
			@ForAll @IntRange(min = 1, max = 364) int atRiskThreshold,
			@ForAll @IntRange(min = 2, max = 365) int overdueThreshold) {

		Assume.that(atRiskThreshold < overdueThreshold);

		EngagementStatus status = EngagementClassifier.classify(recency, atRiskThreshold, overdueThreshold);
		boolean followUp = EngagementClassifier.needsFollowUp(status);

		assertThat(followUp).isEqualTo(status != EngagementStatus.ON_TRACK);
	}

	@Property(tries = 200)
	void followUpFlagForNullRecencyIsTrue(
			@ForAll @IntRange(min = 1, max = 364) int atRiskThreshold,
			@ForAll @IntRange(min = 2, max = 365) int overdueThreshold) {

		Assume.that(atRiskThreshold < overdueThreshold);

		EngagementStatus status = EngagementClassifier.classify(null, atRiskThreshold, overdueThreshold);
		assertThat(EngagementClassifier.needsFollowUp(status)).isTrue();
	}
}
