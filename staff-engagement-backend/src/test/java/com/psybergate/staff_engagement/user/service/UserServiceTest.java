package com.psybergate.staff_engagement.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.psybergate.staff_engagement.user.domain.User;
import com.psybergate.staff_engagement.user.domain.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	private UserRepository userRepository;

	private UserService service() {
		return new UserServiceImpl(userRepository);
	}

	private User user(long id, String name, String email) {
		User user = new User();
		user.setId(id);
		user.setName(name);
		user.setEmail(email);
		return user;
	}

	@Test
	void listAll_returnsEveryUser() {
		User alice = user(1L, "Alice Johnson", "alice@example.com");
		User bob = user(2L, "Bob Smith", "bob@example.com");
		when(userRepository.findAll()).thenReturn(List.of(alice, bob));

		assertThat(service().listAll()).containsExactly(alice, bob);
	}

	@Test
	void listAll_withNoUsers_returnsEmptyList() {
		when(userRepository.findAll()).thenReturn(List.of());

		assertThat(service().listAll()).isEmpty();
	}

	@Test
	void findByEmail_knownEmail_returnsUser() {
		User alice = user(1L, "Alice Johnson", "alice@example.com");
		when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice));

		assertThat(service().findByEmail("alice@example.com")).contains(alice);
	}

	@Test
	void findByEmail_unknownEmail_returnsEmpty() {
		when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

		assertThat(service().findByEmail("nobody@example.com")).isEmpty();
	}
}
