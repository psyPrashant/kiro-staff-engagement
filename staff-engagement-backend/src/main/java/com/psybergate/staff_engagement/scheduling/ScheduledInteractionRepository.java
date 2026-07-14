package com.psybergate.staff_engagement.scheduling;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ScheduledInteractionRepository extends JpaRepository<ScheduledInteraction, Long> {

	@Query("""
			SELECT si FROM ScheduledInteraction si
			WHERE si.employee.id = :employeeId
			  AND si.scheduledDate >= :referenceDate
			  AND si.completionStatus = com.psybergate.staff_engagement.scheduling.CompletionStatus.PENDING
			ORDER BY si.scheduledDate ASC, si.id ASC
			LIMIT 1
			""")
	Optional<ScheduledInteraction> findNextPendingByEmployeeId(
			@Param("employeeId") Long employeeId,
			@Param("referenceDate") LocalDate referenceDate);

	@Query("""
			SELECT si FROM ScheduledInteraction si
			WHERE si.employee.id IN :employeeIds
			  AND si.scheduledDate >= :referenceDate
			  AND si.completionStatus = com.psybergate.staff_engagement.scheduling.CompletionStatus.PENDING
			  AND si.scheduledDate = (
			      SELECT MIN(si2.scheduledDate)
			      FROM ScheduledInteraction si2
			      WHERE si2.employee.id = si.employee.id
			        AND si2.scheduledDate >= :referenceDate
			        AND si2.completionStatus = com.psybergate.staff_engagement.scheduling.CompletionStatus.PENDING
			  )
			ORDER BY si.employee.id ASC, si.id ASC
			""")
	List<ScheduledInteraction> findNextPendingByEmployeeIds(
			@Param("employeeIds") List<Long> employeeIds,
			@Param("referenceDate") LocalDate referenceDate);

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
