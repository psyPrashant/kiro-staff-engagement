package com.psybergate.staff_engagement.scheduling;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ScheduledInteractionRepository extends JpaRepository<ScheduledInteraction, Long> {

	List<ScheduledInteraction> findByScheduledByIdOrderByScheduledDateAsc(Long userId);

	List<ScheduledInteraction> findByScheduledByIdAndCompletionStatusOrderByScheduledDateAsc(
			Long userId, CompletionStatus status);

	List<ScheduledInteraction> findByScheduledByIdAndEmployeeIdOrderByScheduledDateAsc(
			Long userId, Long employeeId);

	List<ScheduledInteraction> findByScheduledByIdAndCompletionStatusAndEmployeeIdOrderByScheduledDateAsc(
			Long userId, CompletionStatus status, Long employeeId);

	long countByScheduledByIdAndCompletionStatusAndScheduledDateBefore(
			Long userId, CompletionStatus status, LocalDate date);
}
