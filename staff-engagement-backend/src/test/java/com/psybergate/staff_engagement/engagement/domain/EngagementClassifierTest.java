package com.psybergate.staff_engagement.engagement.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.psybergate.staff_engagement.engagement.domain.EngagementStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link EngagementClassifier} covering boundary cases.
 * Validates Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 7.9
 */
class EngagementClassifierTest {

	@Nested
	@DisplayName("classify()")
	class Classify {

		@Test
		@DisplayName("null recency → OVERDUE (no interactions)")
		void nullRecency_returnsOverdue() {
			EngagementStatus result = EngagementClassifier.classify(null, 14, 30);
			assertThat(result).isEqualTo(EngagementStatus.OVERDUE);
		}

		@Test
		@DisplayName("recency exactly at overdue threshold → OVERDUE")
		void recencyAtOverdueThreshold_returnsOverdue() {
			EngagementStatus result = EngagementClassifier.classify(30, 14, 30);
			assertThat(result).isEqualTo(EngagementStatus.OVERDUE);
		}

		@Test
		@DisplayName("recency above overdue threshold → OVERDUE")
		void recencyAboveOverdueThreshold_returnsOverdue() {
			EngagementStatus result = EngagementClassifier.classify(31, 14, 30);
			assertThat(result).isEqualTo(EngagementStatus.OVERDUE);
		}

		@Test
		@DisplayName("recency exactly at at-risk threshold → AT_RISK")
		void recencyAtAtRiskThreshold_returnsAtRisk() {
			EngagementStatus result = EngagementClassifier.classify(14, 14, 30);
			assertThat(result).isEqualTo(EngagementStatus.AT_RISK);
		}

		@Test
		@DisplayName("recency between thresholds → AT_RISK")
		void recencyBetweenThresholds_returnsAtRisk() {
			EngagementStatus result = EngagementClassifier.classify(15, 14, 30);
			assertThat(result).isEqualTo(EngagementStatus.AT_RISK);
		}

		@Test
		@DisplayName("recency one day below at-risk threshold → ON_TRACK")
		void recencyOneDayBelowAtRisk_returnsOnTrack() {
			EngagementStatus result = EngagementClassifier.classify(13, 14, 30);
			assertThat(result).isEqualTo(EngagementStatus.ON_TRACK);
		}

		@Test
		@DisplayName("recency 0 (interaction today) → ON_TRACK")
		void recencyZero_returnsOnTrack() {
			EngagementStatus result = EngagementClassifier.classify(0, 14, 30);
			assertThat(result).isEqualTo(EngagementStatus.ON_TRACK);
		}
	}

	@Nested
	@DisplayName("needsFollowUp()")
	class NeedsFollowUp {

		@Test
		@DisplayName("OVERDUE → true")
		void overdue_returnsTrue() {
			assertThat(EngagementClassifier.needsFollowUp(EngagementStatus.OVERDUE)).isTrue();
		}

		@Test
		@DisplayName("AT_RISK → true")
		void atRisk_returnsTrue() {
			assertThat(EngagementClassifier.needsFollowUp(EngagementStatus.AT_RISK)).isTrue();
		}

		@Test
		@DisplayName("ON_TRACK → false")
		void onTrack_returnsFalse() {
			assertThat(EngagementClassifier.needsFollowUp(EngagementStatus.ON_TRACK)).isFalse();
		}
	}
}
