package com.psybergate.staff_engagement.auth.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.psybergate.staff_engagement.user.domain.User;
import com.psybergate.staff_engagement.user.service.UserService;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CurrentUserResolverTest {

	@Mock
	private UserService userService;

	private CurrentUserResolver resolver;

	@BeforeEach
	void setUp() {
		resolver = new CurrentUserResolver(userService);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void resolve_authenticatedUser_returnsUser() {
		// Arrange
		String email = "test@email.com";
		User expectedUser = new User();
		expectedUser.setId(1L);
		expectedUser.setName("Test User");
		expectedUser.setEmail(email);

		Authentication authentication = mock(Authentication.class);
		when(authentication.isAuthenticated()).thenReturn(true);
		when(authentication.getName()).thenReturn(email);

		SecurityContext securityContext = mock(SecurityContext.class);
		when(securityContext.getAuthentication()).thenReturn(authentication);
		SecurityContextHolder.setContext(securityContext);

		when(userService.findByEmail(email)).thenReturn(Optional.of(expectedUser));

		// Act
		User result = resolver.resolve();

		// Assert
		assertThat(result).isEqualTo(expectedUser);
		assertThat(result.getEmail()).isEqualTo(email);
		assertThat(result.getName()).isEqualTo("Test User");
		assertThat(result.getId()).isEqualTo(1L);
	}

	@Test
	void resolve_noAuthentication_throws401() {
		// Arrange
		SecurityContext securityContext = mock(SecurityContext.class);
		when(securityContext.getAuthentication()).thenReturn(null);
		SecurityContextHolder.setContext(securityContext);

		// Act & Assert
		assertThatThrownBy(() -> resolver.resolve())
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(ex -> {
					ResponseStatusException rse = (ResponseStatusException) ex;
					assertThat(rse.getStatusCode().value()).isEqualTo(401);
				});
	}

	@Test
	void resolve_authenticatedButUserDeletedFromDb_throws401() {
		// Arrange
		String email = "deleted@email.com";

		Authentication authentication = mock(Authentication.class);
		when(authentication.isAuthenticated()).thenReturn(true);
		when(authentication.getName()).thenReturn(email);

		SecurityContext securityContext = mock(SecurityContext.class);
		when(securityContext.getAuthentication()).thenReturn(authentication);
		SecurityContextHolder.setContext(securityContext);

		when(userService.findByEmail(email)).thenReturn(Optional.empty());

		// Act & Assert
		assertThatThrownBy(() -> resolver.resolve())
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(ex -> {
					ResponseStatusException rse = (ResponseStatusException) ex;
					assertThat(rse.getStatusCode().value()).isEqualTo(401);
				});
	}
}
