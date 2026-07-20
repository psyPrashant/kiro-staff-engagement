package com.psybergate.staff_engagement.client.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
public class Project {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 255)
	private String name;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "company_id", nullable = false)
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
	private Company company;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	protected void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}
}
