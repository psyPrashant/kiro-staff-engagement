package com.psybergate.staff_engagement.task;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

	List<Task> findByInteractionIdInAndStatus(List<Long> interactionIds, TaskStatus status);
}
