package com.psybergate.staff_engagement.scheduling;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NextScheduledInteractionService {

	private final ScheduledInteractionRepository repository;
	private final Clock clock;

	@Transactional(readOnly = true)
	public NextScheduledDto getNextScheduled(Long employeeId) {
		if (employeeId == null) {
			throw new IllegalArgumentException("employeeId must not be null");
		}
		LocalDate today = LocalDate.now(clock);
		return repository.findNextPendingByEmployeeId(employeeId, today)
				.map(this::toDto)
				.orElse(null);
	}

	@Transactional(readOnly = true)
	public Map<Long, NextScheduledDto> getNextScheduledBatch(List<Long> employeeIds) {
		if (employeeIds == null) {
			throw new IllegalArgumentException("employeeIds must not be null");
		}
		if (employeeIds.isEmpty()) {
			return Map.of();
		}
		if (employeeIds.size() > 200) {
			throw new IllegalArgumentException("batch size must not exceed 200");
		}

		LocalDate today = LocalDate.now(clock);
		List<ScheduledInteraction> results =
				repository.findNextPendingByEmployeeIds(employeeIds, today);

		Map<Long, NextScheduledDto> map = new HashMap<>();
		for (ScheduledInteraction si : results) {
			Long empId = si.getEmployee().getId();
			if (!map.containsKey(empId)) {
				map.put(empId, toDto(si));
			}
		}
		return map;
	}

	private NextScheduledDto toDto(ScheduledInteraction si) {
		return new NextScheduledDto(
				si.getScheduledDate().toString(),
				si.getInteractionType().name()
		);
	}
}
