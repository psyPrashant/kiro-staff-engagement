package com.psybergate.staff_engagement.auth;

import com.psybergate.staff_engagement.user.User;
import com.psybergate.staff_engagement.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final AuthenticationManager authenticationManager;
	private final UserRepository userRepository;

	public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
		Authentication auth = authenticationManager.authenticate(
			new UsernamePasswordAuthenticationToken(request.email(), request.password())
		);

		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(auth);
		SecurityContextHolder.setContext(context);

		HttpSession session = httpRequest.getSession(true);
		session.setAttribute(
			HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

		User user = userRepository.findByEmail(request.email())
			.orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));

		return new LoginResponse(user.getId(), user.getName(), user.getEmail());
	}
}
