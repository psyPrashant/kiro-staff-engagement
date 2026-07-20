package com.psybergate.staff_engagement.engagement.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "engagement.thresholds")
@Validated
@Getter
@Setter
public class EngagementThresholdProperties {

	@Min(1)
	@Max(365)
	private int overdueDays = 30;

	@Min(1)
	@Max(365)
	private int atRiskDays = 14;

	@PostConstruct
	void validateThresholdRelationship() {
		if (atRiskDays >= overdueDays) {
			throw new IllegalStateException(
					"at-risk threshold must be strictly less than overdue threshold");
		}
	}
}
