package com.psybergate.staff_engagement.greeting;

import com.psybergate.staff_engagement.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class GreetingRepositoryIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private GreetingRepository greetingRepository;

	@Test
	void saveAndFindById() {
		Greeting greeting = Greeting.builder()
				.message("Hello, World!")
				.createdAt(LocalDateTime.now())
				.build();

		Greeting saved = greetingRepository.save(greeting);

		assertThat(saved.getId()).isNotNull();

		Optional<Greeting> found = greetingRepository.findById(saved.getId());

		assertThat(found).isPresent();
		assertThat(found.get().getMessage()).isEqualTo("Hello, World!");
		assertThat(found.get().getCreatedAt()).isNotNull();
	}
}
