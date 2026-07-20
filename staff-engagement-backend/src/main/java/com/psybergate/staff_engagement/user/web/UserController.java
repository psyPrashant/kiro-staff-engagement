package com.psybergate.staff_engagement.user.web;

import com.psybergate.staff_engagement.user.domain.User;
import com.psybergate.staff_engagement.user.service.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@GetMapping("/api/users")
	public List<User> getAllUsers() {
		return userService.listAll();
	}
}
