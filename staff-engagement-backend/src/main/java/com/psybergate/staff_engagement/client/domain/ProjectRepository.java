package com.psybergate.staff_engagement.client.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {

	List<Project> findByCompanyId(Long companyId);
}
