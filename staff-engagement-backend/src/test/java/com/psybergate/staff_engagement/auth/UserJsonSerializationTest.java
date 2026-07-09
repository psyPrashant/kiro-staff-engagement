package com.psybergate.staff_engagement.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.psybergate.staff_engagement.user.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test verifying that the User entity's passwordHash field
 * is excluded from JSON serialization via @JsonIgnore.
 *
 * Validates: Requirements 1.4
 */
class UserJsonSerializationTest {

	private final ObjectMapper objectMapper = new ObjectMapper()
			.registerModule(new JavaTimeModule());

	@Test
	void passwordHash_isExcludedFromJsonSerialization() throws Exception {
		User user = new User();
		user.setId(1L);
		user.setName("Alice Johnson");
		user.setEmail("alice.johnson@psybergate.com");
		user.setPasswordHash("$2a$10$someHashedPasswordValueHere1234567890abcdef");
		user.setCreatedAt(Instant.parse("2024-01-15T10:30:00Z"));

		String json = objectMapper.writeValueAsString(user);

		assertThat(json).doesNotContain("passwordHash");
		assertThat(json).doesNotContain("password_hash");

		// Verify other fields ARE present in the serialized output
		assertThat(json).contains("\"id\"");
		assertThat(json).contains("\"name\"");
		assertThat(json).contains("\"email\"");
		assertThat(json).contains("\"createdAt\"");
	}
}
