package com.psybergate.staff_engagement.engagement.service;

import com.psybergate.staff_engagement.engagement.domain.EngagementStatus;
import com.psybergate.staff_engagement.engagement.dto.EngagementMatrixEntry;
import java.time.LocalDate;
import java.util.List;

/**
 * Computes the engagement matrix — per-employee interaction recency, frequency
 * and the resulting engagement status.
 */
public interface EngagementService {

	/**
	 * Computes one matrix entry per employee, optionally filtered by status and
	 * ordered by the given key.
	 *
	 * @param referenceDate the date recency is measured against; {@code null} means today
	 * @param statusFilter  keep only entries with this status; {@code null} keeps all
	 * @param sortOrder     {@code "recency"} sorts by days-since-last-interaction descending;
	 *                      any other value sorts by employee name, case-insensitively
	 * @return the matrix entries
	 */
	List<EngagementMatrixEntry> computeMatrix(
			LocalDate referenceDate, EngagementStatus statusFilter, String sortOrder);
}
