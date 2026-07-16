package com.psybergate.staff_engagement.scheduling;

import com.psybergate.staff_engagement.employee.Employee;
import com.psybergate.staff_engagement.employee.EmployeeRepository;
import com.psybergate.staff_engagement.user.User;
import com.psybergate.staff_engagement.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SchedulingService {

	private static final int MAX_LIST_ENTRIES = 200;

	private final ScheduledInteractionRepository repository;
	private final EmployeeRepository employeeRepository;
	private final UserRepository userRepository;
	private final Clock clock;

	@Transactional
	public ScheduledInteractionResponse create(CreateScheduledInteractionRequest request, Long userId) {
		validateScheduledDate(request.scheduledDate());

		Employee employee = employeeRepository.findById(request.employeeId())
				.orElseThrow(() -> new EmployeeNotFoundException(request.employeeId()));

		User scheduledBy = userRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

		ScheduledInteraction entity = new ScheduledInteraction();
		entity.setEmployee(employee);
		entity.setScheduledBy(scheduledBy);
		entity.setScheduledDate(request.scheduledDate());
		entity.setInteractionType(request.interactionType());
		entity.setNotes(request.notes());
		entity.setCompletionStatus(CompletionStatus.PENDING);

		ScheduledInteraction saved = repository.save(entity);
		return toResponse(saved);
	}

	@Transactional(readOnly = true)
	public List<ScheduledInteractionResponse> list(Long userId, CompletionStatus status,
			Long employeeId, Boolean overdue) {
		List<ScheduledInteraction> results = queryWithFilters(userId, status, employeeId);

		LocalDate today = LocalDate.now(clock);

		return results.stream()
				.filter(entry -> {
					if (Boolean.TRUE.equals(overdue)) {
						return entry.getCompletionStatus() == CompletionStatus.PENDING
								&& entry.getScheduledDate().isBefore(today);
					}
					return true;
				})
				.limit(MAX_LIST_ENTRIES)
				.map(this::toResponse)
				.collect(Collectors.toList());
	}

	@Transactional
	public ScheduledInteractionResponse update(Long id, UpdateScheduledInteractionRequest request, Long userId) {
		ScheduledInteraction entity = repository.findById(id)
				.filter(e -> e.getScheduledBy().getId().equals(userId))
				.orElseThrow(() -> new ScheduledInteractionNotFoundException(id));

		validateStatusTransition(entity.getCompletionStatus(), request.completionStatus());

		if (request.scheduledDate() != null) {
			validateScheduledDate(request.scheduledDate());
			entity.setScheduledDate(request.scheduledDate());
		}

		if (request.notes() != null) {
			entity.setNotes(request.notes());
		}

		if (request.completionStatus() != null) {
			entity.setCompletionStatus(request.completionStatus());
		}

		ScheduledInteraction saved = repository.save(entity);
		return toResponse(saved);
	}

	@Transactional
	public void delete(Long id, Long userId) {
		ScheduledInteraction entity = repository.findById(id)
				.filter(e -> e.getScheduledBy().getId().equals(userId))
				.orElseThrow(() -> new ScheduledInteractionNotFoundException(id));
		repository.delete(entity);
	}

	public long countOverdue(Long userId) {
		return repository.countByScheduledByIdAndCompletionStatusAndScheduledDateBefore(
				userId, CompletionStatus.PENDING, LocalDate.now(clock));
	}

	public boolean isOverdue(LocalDate scheduledDate, CompletionStatus status) {
		return status == CompletionStatus.PENDING
				&& scheduledDate.isBefore(LocalDate.now(clock));
	}

	private void validateScheduledDate(LocalDate date) {
		if (date.isBefore(LocalDate.now(clock))) {
			throw new IllegalArgumentException("Scheduled date must be today or in the future");
		}
	}

	private void validateStatusTransition(CompletionStatus current, CompletionStatus target) {
		if (target == null) {
			return;
		}
		if (current != CompletionStatus.PENDING) {
			throw new IllegalStateException(
					"Cannot transition from " + current + " to " + target
							+ ". Only PENDING interactions can be modified.");
		}
		if (target != CompletionStatus.COMPLETED && target != CompletionStatus.CANCELLED) {
			throw new IllegalStateException(
					"Invalid status transition from " + current + " to " + target
							+ ". Only PENDING→COMPLETED and PENDING→CANCELLED are allowed.");
		}
	}

	private List<ScheduledInteraction> queryWithFilters(Long userId, CompletionStatus status, Long employeeId) {
		if (status != null && employeeId != null) {
			return repository.findByScheduledByIdAndCompletionStatusAndEmployeeIdOrderByScheduledDateAsc(
					userId, status, employeeId);
		}
		if (status != null) {
			return repository.findByScheduledByIdAndCompletionStatusOrderByScheduledDateAsc(userId, status);
		}
		if (employeeId != null) {
			return repository.findByScheduledByIdAndEmployeeIdOrderByScheduledDateAsc(userId, employeeId);
		}
		return repository.findByScheduledByIdOrderByScheduledDateAsc(userId);
	}

	private ScheduledInteractionResponse toResponse(ScheduledInteraction entity) {
		LocalDate today = LocalDate.now(clock);
		boolean overdue = entity.getCompletionStatus() == CompletionStatus.PENDING
				&& entity.getScheduledDate().isBefore(today);

		return new ScheduledInteractionResponse(
				entity.getId(),
				entity.getEmployee().getId(),
				entity.getEmployee().getName(),
				entity.getScheduledDate(),
				entity.getInteractionType(),
				entity.getCompletionStatus(),
				entity.getNotes(),
				overdue,
				entity.getCreatedAt()
		);
	}
}
