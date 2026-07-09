package com.psybergate.staff_engagement.auth;

import com.psybergate.staff_engagement.user.User;
import com.psybergate.staff_engagement.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class CurrentUserResolver {

	private final UserRepository userRepository;

	public User resolve() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
		}
		String email = auth.getName();
		return userRepository.findByEmail(email)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
	}
}
