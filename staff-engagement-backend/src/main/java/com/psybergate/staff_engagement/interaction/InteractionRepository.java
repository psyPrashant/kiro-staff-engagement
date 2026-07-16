package com.psybergate.staff_engagement.interaction;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.psybergate.staff_engagement.employee.Employee;

public interface InteractionRepository extends JpaRepository<Interaction, Long> {

	@Query("""
			SELECT i.employee.id, MAX(i.occurredAt), COUNT(i)
			FROM Interaction i
			GROUP BY i.employee.id
			""")
	List<Object[]> findInteractionAggregatesByEmployee();

	List<Interaction> findByEmployeeIdOrderByOccurredAtDesc(Long employeeId);

	// Number of distinct employees that have an interaction against each project.
	// Used to derive the "Employees Assigned" count on the Companies/Projects list,
	// since employees relate to a project only through their interactions.
	@Query("""
			SELECT i.project.id, COUNT(DISTINCT i.employee.id)
			FROM Interaction i
			WHERE i.project.id IS NOT NULL
			GROUP BY i.project.id
			""")
	List<Object[]> countDistinctEmployeesGroupedByProject();

	// Distinct employees that have an interaction against a given project.
	@Query("""
			SELECT DISTINCT i.employee
			FROM Interaction i
			WHERE i.project.id = :projectId
			ORDER BY i.employee.name
			""")
	List<Employee> findDistinctEmployeesByProjectId(@Param("projectId") Long projectId);
}
