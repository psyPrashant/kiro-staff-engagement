package com.psybergate.staff_engagement.interaction;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InteractionRepository extends JpaRepository<Interaction, Long> {

	@Query("""
			SELECT i.employee.id, MAX(i.occurredAt), COUNT(i)
			FROM Interaction i
			GROUP BY i.employee.id
			""")
	List<Object[]> findInteractionAggregatesByEmployee();

	List<Interaction> findByEmployeeIdOrderByOccurredAtDesc(Long employeeId);
}
