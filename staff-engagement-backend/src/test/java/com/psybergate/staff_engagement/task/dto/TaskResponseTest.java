package com.psybergate.staff_engagement.task.dto;

import static org.junit.jupiter.api.Assertions.*;

import com.psybergate.staff_engagement.employee.domain.Employee;
import com.psybergate.staff_engagement.interaction.domain.Interaction;
import com.psybergate.staff_engagement.task.domain.Task;
import com.psybergate.staff_engagement.task.domain.TaskStatus;
import com.psybergate.staff_engagement.user.domain.User;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TaskResponseTest {

	@Test
	void from_withAllAssociationsPresent_mapsAllFieldsCorrectly() {
		Employee employee = new Employee();
		employee.setId(10L);
		employee.setName("Jane Doe");

		User assignedUser = new User();
		assignedUser.setId(20L);
		assignedUser.setName("John Smith");

		Interaction interaction = new Interaction();
		interaction.setId(30L);

		Instant createdAt = Instant.parse("2024-06-15T10:30:00Z");

		Task task = new Task();
		task.setId(1L);
		task.setTitle("Follow up with employee");
		task.setDescription("Schedule a check-in meeting");
		task.setStatus(TaskStatus.OPEN);
		task.setDueDate(LocalDate.of(2024, 7, 1));
		task.setAssignedUser(assignedUser);
		task.setInteraction(interaction);
		task.setEmployee(employee);
		task.setCreatedAt(createdAt);

		TaskResponse response = TaskResponse.from(task);

		assertEquals(1L, response.id());
		assertEquals("Follow up with employee", response.title());
		assertEquals("Schedule a check-in meeting", response.description());
		assertEquals("OPEN", response.status());
		assertEquals(LocalDate.of(2024, 7, 1), response.dueDate());
		assertEquals(20L, response.assignedUserId());
		assertEquals("John Smith", response.assignedUserName());
		assertEquals(30L, response.interactionId());
		assertEquals(10L, response.employeeId());
		assertEquals("Jane Doe", response.employeeName());
		assertEquals(createdAt, response.createdAt());
	}

	@Test
	void from_withNullEmployee_mapsEmployeeFieldsAsNull() {
		User assignedUser = new User();
		assignedUser.setId(20L);
		assignedUser.setName("John Smith");

		Interaction interaction = new Interaction();
		interaction.setId(30L);

		Instant createdAt = Instant.parse("2024-06-15T10:30:00Z");

		Task task = new Task();
		task.setId(2L);
		task.setTitle("Review document");
		task.setDescription("Review the Q2 report");
		task.setStatus(TaskStatus.DONE);
		task.setDueDate(LocalDate.of(2024, 6, 30));
		task.setAssignedUser(assignedUser);
		task.setInteraction(interaction);
		task.setEmployee(null);
		task.setCreatedAt(createdAt);

		TaskResponse response = TaskResponse.from(task);

		assertEquals(2L, response.id());
		assertEquals("Review document", response.title());
		assertEquals("Review the Q2 report", response.description());
		assertEquals("DONE", response.status());
		assertEquals(LocalDate.of(2024, 6, 30), response.dueDate());
		assertEquals(20L, response.assignedUserId());
		assertEquals("John Smith", response.assignedUserName());
		assertEquals(30L, response.interactionId());
		assertNull(response.employeeId());
		assertNull(response.employeeName());
		assertEquals(createdAt, response.createdAt());
	}

	@Test
	void from_withNullAssignedUserAndNullInteraction_mapsRespectiveFieldsAsNull() {
		Employee employee = new Employee();
		employee.setId(10L);
		employee.setName("Jane Doe");

		Instant createdAt = Instant.parse("2024-06-15T10:30:00Z");

		Task task = new Task();
		task.setId(3L);
		task.setTitle("Standalone task");
		task.setDescription(null);
		task.setStatus(TaskStatus.OPEN);
		task.setDueDate(null);
		task.setAssignedUser(null);
		task.setInteraction(null);
		task.setEmployee(employee);
		task.setCreatedAt(createdAt);

		TaskResponse response = TaskResponse.from(task);

		assertEquals(3L, response.id());
		assertEquals("Standalone task", response.title());
		assertNull(response.description());
		assertEquals("OPEN", response.status());
		assertNull(response.dueDate());
		assertNull(response.assignedUserId());
		assertNull(response.assignedUserName());
		assertNull(response.interactionId());
		assertEquals(10L, response.employeeId());
		assertEquals("Jane Doe", response.employeeName());
		assertEquals(createdAt, response.createdAt());
	}
}
