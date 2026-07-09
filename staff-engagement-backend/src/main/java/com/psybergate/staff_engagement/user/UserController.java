package com.psybergate.staff_engagement.user;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserController {

	private final UserRepository userRepository;

	@GetMapping("/api/users")
	public List<User> getAllUsers() {
		return userRepository.findAll();
	}
}
