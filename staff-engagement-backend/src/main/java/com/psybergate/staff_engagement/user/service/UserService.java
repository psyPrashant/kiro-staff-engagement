package com.psybergate.staff_engagement.user.service;

import com.psybergate.staff_engagement.user.domain.User;
import java.util.List;
import java.util.Optional;

/**
 * Read operations for application users — the people who conduct and log
 * interactions, as opposed to the employees being engaged with.
 */
public interface UserService {

	/**
	 * Lists every user.
	 *
	 * @return all users
	 */
	List<User> listAll();

	/**
	 * Looks a user up by their email address, which is also their login identity.
	 *
	 * @param email the email to look up
	 * @return the matching user, or empty if there is none
	 */
	Optional<User> findByEmail(String email);
}
