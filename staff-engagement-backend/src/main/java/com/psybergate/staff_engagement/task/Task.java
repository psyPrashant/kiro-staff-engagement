package com.psybergate.staff_engagement.task;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.psybergate.staff_engagement.employee.Employee;
import com.psybergate.staff_engagement.interaction.Interaction;
import com.psybergate.staff_engagement.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
public class Task {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "interaction_id")
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
	private Interaction interaction;

	@Column(nullable = false, length = 255)
	private String title;

	@Column(length = 2000)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TaskStatus status = TaskStatus.OPEN;

	@Column(name = "due_date")
	private LocalDate dueDate;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "employee_id")
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
	private Employee employee;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assigned_user_id")
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
	private User assignedUser;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	protected void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}
}
