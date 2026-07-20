package com.psybergate.staff_engagement.engagement.domain;

import com.psybergate.staff_engagement.engagement.domain.EngagementStatus;

/**
 * Pure classification utility for employee engagement status.
 * Stateless and side-effect free — suitable for property-based testing.
 */
public final class EngagementClassifier {

	private EngagementClassifier() {}

	/**
	 * Classifies an employee's engagement status based on recency and thresholds.
	 *
	 * @param recency           days since last interaction, or null if no interactions
	 * @param atRiskThreshold   days threshold for AT_RISK classification
	 * @param overdueThreshold  days threshold for OVERDUE classification
	 * @return the computed EngagementStatus
	 */
	public static EngagementStatus classify(Integer recency, int atRiskThreshold, int overdueThreshold) {
		if (recency == null || recency >= overdueThreshold) {
			return EngagementStatus.OVERDUE;
		}
		if (recency >= atRiskThreshold) {
			return EngagementStatus.AT_RISK;
		}
		return EngagementStatus.ON_TRACK;
	}

	/**
	 * Derives the follow-up flag from engagement status.
	 *
	 * @param status the engagement status
	 * @return true if follow-up is needed (OVERDUE or AT_RISK), false otherwise
	 */
	public static boolean needsFollowUp(EngagementStatus status) {
		return status != EngagementStatus.ON_TRACK;
	}
}
