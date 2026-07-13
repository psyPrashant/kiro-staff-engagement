package com.psybergate.staff_engagement.scheduling;

import com.psybergate.staff_engagement.employee.Employee;
import com.psybergate.staff_engagement.interaction.InteractionType;
import com.psybergate.staff_engagement.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "scheduled_interactions")
@Getter
@Setter
@NoArgsConstructor
public class ScheduledInteraction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "employee_id", nullable = false)
	private Employee employee;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "scheduled_by_user_id", nullable = false)
	private User scheduledBy;

	@Column(name = "scheduled_date", nullable = false)
	private LocalDate scheduledDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "interaction_type", nullable = false)
	private InteractionType interactionType;

	@Column(length = 2000)
	private String notes;

	@Enumerated(EnumType.STRING)
	@Column(name = "completion_status", nullable = false)
	private CompletionStatus completionStatus = CompletionStatus.PENDING;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	protected void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}
}
