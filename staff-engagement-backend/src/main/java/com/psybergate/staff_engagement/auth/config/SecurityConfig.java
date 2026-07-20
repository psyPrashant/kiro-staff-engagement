package com.psybergate.staff_engagement.auth.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.csrf(csrf -> csrf.disable())
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
				.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
				.requestMatchers("/api/**").authenticated()
				.anyRequest().permitAll()
			)
			.exceptionHandling(ex -> ex
				.authenticationEntryPoint(unauthorizedEntryPoint())
			)
			.logout(logout -> logout
				.logoutUrl("/api/auth/logout")
				.logoutSuccessHandler(logoutSuccessHandler())
				.invalidateHttpSession(true)
				.deleteCookies("JSESSIONID")
			)
			.sessionManagement(session -> session
				.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
			);
		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(10);
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
		return authConfig.getAuthenticationManager();
	}

	private AuthenticationEntryPoint unauthorizedEntryPoint() {
		return (request, response, authException) -> {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			response.getWriter().write("{\"error\":\"Authentication required\"}");
		};
	}

	private LogoutSuccessHandler logoutSuccessHandler() {
		return (request, response, authentication) -> {
			response.setStatus(HttpServletResponse.SC_OK);
		};
	}
}
