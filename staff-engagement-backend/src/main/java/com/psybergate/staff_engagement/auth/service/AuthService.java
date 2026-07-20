package com.psybergate.staff_engagement.auth.service;

import com.psybergate.staff_engagement.auth.dto.LoginRequest;
import com.psybergate.staff_engagement.auth.dto.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Session-based authentication operations.
 */
public interface AuthService {

	/**
	 * Authenticates the supplied credentials and establishes a server-side session,
	 * storing the resulting security context against the HTTP session.
	 *
	 * @param request     the submitted credentials
	 * @param httpRequest the current request, used to create the session
	 * @return the authenticated user's identity
	 * @throws org.springframework.security.core.AuthenticationException if the credentials are invalid
	 */
	LoginResponse login(LoginRequest request, HttpServletRequest httpRequest);
}
