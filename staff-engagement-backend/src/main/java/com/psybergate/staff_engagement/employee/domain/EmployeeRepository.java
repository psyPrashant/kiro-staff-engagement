package com.psybergate.staff_engagement.employee.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

	List<Employee> findByManagerId(Long managerId);
}
