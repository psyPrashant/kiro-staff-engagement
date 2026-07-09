package com.psybergate.staff_engagement.auth;

import com.psybergate.staff_engagement.user.User;
import com.psybergate.staff_engagement.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private CustomUserDetailsService customUserDetailsService;

	@Test
	@DisplayName("loadUserByUsername returns UserDetails with email as username and passwordHash as password")
	void loadUserByUsername_userFound_returnsCorrectUserDetails() {
		String email = "alice@psybergate.com";
		String passwordHash = "$2a$10$abcdefghijklmnopqrstuuABCDEFGHIJKLMNOPQRSTUVWXYZ12345";

		User user = new User();
		user.setEmail(email);
		user.setPasswordHash(passwordHash);

		when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

		UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

		assertThat(userDetails.getUsername()).isEqualTo(email);
		assertThat(userDetails.getPassword()).isEqualTo(passwordHash);
	}

	@Test
	@DisplayName("loadUserByUsername throws UsernameNotFoundException when user not found")
	void loadUserByUsername_userNotFound_throwsUsernameNotFoundException() {
		String email = "unknown@psybergate.com";

		when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(email))
				.isInstanceOf(UsernameNotFoundException.class)
				.hasMessage("Invalid credentials");
	}
}
