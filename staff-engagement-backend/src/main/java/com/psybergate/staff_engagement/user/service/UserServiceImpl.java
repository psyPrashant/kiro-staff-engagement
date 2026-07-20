package com.psybergate.staff_engagement.user.service;

import com.psybergate.staff_engagement.user.domain.User;
import com.psybergate.staff_engagement.user.domain.UserRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;

	@Override
	public List<User> listAll() {
		return userRepository.findAll();
	}

	@Override
	public Optional<User> findByEmail(String email) {
		return userRepository.findByEmail(email);
	}
}
