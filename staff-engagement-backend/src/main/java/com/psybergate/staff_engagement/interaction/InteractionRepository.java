package com.psybergate.staff_engagement.interaction;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InteractionRepository extends JpaRepository<Interaction, Long> {

	List<Interaction> findByEmployeeIdOrderByOccurredAtDesc(Long employeeId);
}
