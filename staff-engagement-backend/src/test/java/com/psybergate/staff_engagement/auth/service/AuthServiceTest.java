package com.psybergate.staff_engagement.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.psybergate.staff_engagement.auth.dto.LoginRequest;
import com.psybergate.staff_engagement.auth.dto.LoginResponse;
import com.psybergate.staff_engagement.user.domain.User;
import com.psybergate.staff_engagement.user.domain.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private UserRepository userRepository;

	@Mock
	private HttpServletRequest httpServletRequest;

	@Mock
	private HttpSession httpSession;

	@InjectMocks
	private AuthServiceImpl authService;

	@BeforeEach
	void setUp() {
		lenient().when(httpServletRequest.getSession(true)).thenReturn(httpSession);
	}

	@Test
	void login_withValidCredentials_returnsLoginResponse() {
		// Arrange
		String email = "alice.johnson@psybergate.com";
		String password = "Password1";
		LoginRequest request = new LoginRequest(email, password);

		Authentication authentication = mock(Authentication.class);
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
			.thenReturn(authentication);

		User user = new User();
		user.setId(1L);
		user.setName("Alice Johnson");
		user.setEmail(email);
		when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

		// Act
		LoginResponse response = authService.login(request, httpServletRequest);

		// Assert
		assertThat(response).isNotNull();
		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.name()).isEqualTo("Alice Johnson");
		assertThat(response.email()).isEqualTo(email);

		verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
		verify(userRepository).findByEmail(email);
		verify(httpServletRequest).getSession(true);
	}

	@Test
	void login_withInvalidEmail_throwsBadCredentialsException() {
		// Arrange
		String email = "nonexistent@psybergate.com";
		String password = "Password1";
		LoginRequest request = new LoginRequest(email, password);

		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
			.thenThrow(new BadCredentialsException("Invalid credentials"));

		// Act & Assert
		assertThatThrownBy(() -> authService.login(request, httpServletRequest))
			.isInstanceOf(BadCredentialsException.class)
			.hasMessage("Invalid credentials");

		verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
		verifyNoInteractions(userRepository);
	}

	@Test
	void login_withWrongPassword_throwsBadCredentialsException() {
		// Arrange
		String email = "alice.johnson@psybergate.com";
		String password = "WrongPassword";
		LoginRequest request = new LoginRequest(email, password);

		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
			.thenThrow(new BadCredentialsException("Invalid credentials"));

		// Act & Assert
		assertThatThrownBy(() -> authService.login(request, httpServletRequest))
			.isInstanceOf(BadCredentialsException.class)
			.hasMessage("Invalid credentials");

		verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
		verifyNoInteractions(userRepository);
	}
}
