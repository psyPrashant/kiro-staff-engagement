package com.psybergate.staff_engagement.engagement.web;

import com.psybergate.staff_engagement.engagement.domain.EngagementStatus;
import com.psybergate.staff_engagement.engagement.dto.EngagementMatrixEntry;
import com.psybergate.staff_engagement.engagement.service.EngagementService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class EngagementController {

	private final EngagementService engagementService;

	@GetMapping("/api/engagement/matrix")
	public ResponseEntity<List<EngagementMatrixEntry>> getMatrix(
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String sort) {

		EngagementStatus statusFilter = parseStatus(status);
		validateSort(sort);

		List<EngagementMatrixEntry> matrix = engagementService.computeMatrix(null, statusFilter, sort);
		return ResponseEntity.ok(matrix);
	}

	private EngagementStatus parseStatus(String status) {
		if (status == null || status.isBlank()) {
			return null;
		}
		try {
			return EngagementStatus.valueOf(status.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(
					"Invalid status value: '" + status + "'. Valid options are: OVERDUE, AT_RISK, ON_TRACK");
		}
	}

	private void validateSort(String sort) {
		if (sort != null && !sort.isBlank() && !"recency".equalsIgnoreCase(sort)) {
			throw new IllegalArgumentException(
					"Unsupported sort value: '" + sort + "'. Supported options are: recency");
		}
	}
}
