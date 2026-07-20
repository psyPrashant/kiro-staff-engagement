package com.psybergate.staff_engagement.employee.service;

import com.psybergate.staff_engagement.employee.domain.Employee;
import com.psybergate.staff_engagement.employee.domain.EmployeeNotFoundException;
import com.psybergate.staff_engagement.employee.dto.CreateEmployeeRequest;
import com.psybergate.staff_engagement.employee.dto.EmployeeListDto;
import java.util.List;

/**
 * Write operations for employees.
 */
public interface EmployeeService {

	/**
	 * Lists every employee together with their next scheduled interaction, resolved
	 * in a single batch lookup rather than per employee.
	 *
	 * @return one entry per employee
	 */
	List<EmployeeListDto> listAll();

	/**
	 * Reports whether an employee exists.
	 *
	 * @param id the employee to check
	 * @return {@code true} if an employee with that id exists
	 */
	boolean existsById(Long id);

	/**
	 * Creates an employee, trimming the supplied text fields and resolving the
	 * optional manager reference.
	 *
	 * @param request the employee to create
	 * @return the persisted employee
	 * @throws IllegalArgumentException if the referenced manager does not exist
	 */
	Employee create(CreateEmployeeRequest request);

	/**
	 * Deletes an employee together with the records that reference it, so the
	 * removal does not violate foreign-key constraints. Tasks linked either
	 * directly to the employee or to one of the employee's interactions are
	 * removed, along with the employee's interactions and scheduled interactions.
	 * Employees who reported to the deleted employee have their manager reference
	 * cleared.
	 *
	 * @param id the employee to delete
	 * @throws EmployeeNotFoundException if no employee exists with the given id
	 */
	void delete(Long id);
}
