package com.psybergate.staff_engagement.engagement.service;

import com.psybergate.staff_engagement.employee.domain.Employee;
import com.psybergate.staff_engagement.employee.domain.EmployeeRepository;
import com.psybergate.staff_engagement.engagement.config.EngagementThresholdProperties;
import com.psybergate.staff_engagement.engagement.domain.EngagementClassifier;
import com.psybergate.staff_engagement.engagement.domain.EngagementStatus;
import com.psybergate.staff_engagement.engagement.dto.EngagementMatrixEntry;
import com.psybergate.staff_engagement.interaction.domain.InteractionRepository;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EngagementServiceImpl implements EngagementService {

	private final EmployeeRepository employeeRepository;
	private final InteractionRepository interactionRepository;
	private final EngagementThresholdProperties thresholds;
	private final Clock clock;

	@Override
	public List<EngagementMatrixEntry> computeMatrix(
			LocalDate referenceDate,
			EngagementStatus statusFilter,
			String sortOrder) {

		if (referenceDate == null) {
			referenceDate = LocalDate.now(clock);
		}

		List<Employee> employees = employeeRepository.findAll();
		List<Object[]> aggregates = interactionRepository.findInteractionAggregatesByEmployee();

		// Build lookup: employeeId -> (lastOccurredAt, count)
		Map<Long, Object[]> aggregateMap = new HashMap<>();
		for (Object[] row : aggregates) {
			aggregateMap.put((Long) row[0], row);
		}

		List<EngagementMatrixEntry> entries = new ArrayList<>();
		for (Employee emp : employees) {
			Object[] agg = aggregateMap.get(emp.getId());
			Integer recency = null;
			int frequency = 0;
			LocalDate lastInteractionDate = null;

			if (agg != null) {
				Instant lastOccurredAt = (Instant) agg[1];
				long count = (Long) agg[2];
				lastInteractionDate = lastOccurredAt.atZone(ZoneId.systemDefault()).toLocalDate();
				recency = (int) ChronoUnit.DAYS.between(lastInteractionDate, referenceDate);
				frequency = (int) count;
			}

			EngagementStatus status = EngagementClassifier.classify(
					recency, thresholds.getAtRiskDays(), thresholds.getOverdueDays());
			boolean followUp = EngagementClassifier.needsFollowUp(status);

			entries.add(new EngagementMatrixEntry(
					emp.getId(), emp.getName(), emp.getEmail(),
					recency, frequency, lastInteractionDate, status, followUp));
		}

		// Filter
		if (statusFilter != null) {
			entries = entries.stream()
					.filter(e -> e.engagementStatus() == statusFilter)
					.collect(Collectors.toList());
		}

		// Sort
		if ("recency".equalsIgnoreCase(sortOrder)) {
			entries.sort(Comparator.comparing(
					EngagementMatrixEntry::recency,
					Comparator.nullsFirst(Comparator.reverseOrder())));
		} else {
			entries.sort(Comparator.comparing(
					EngagementMatrixEntry::employeeName,
					String.CASE_INSENSITIVE_ORDER));
		}

		return entries;
	}
}
