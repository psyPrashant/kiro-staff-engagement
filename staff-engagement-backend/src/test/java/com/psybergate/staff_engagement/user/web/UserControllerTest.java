package com.psybergate.staff_engagement.user.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.psybergate.staff_engagement.user.domain.User;
import com.psybergate.staff_engagement.user.service.UserService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@WithMockUser
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	@Test
	void getAllUsers_returnsOkWithJsonArray() throws Exception {
		User user1 = new User();
		user1.setId(1L);
		user1.setName("Alice Johnson");
		user1.setEmail("alice@example.com");
		user1.setCreatedAt(Instant.now());

		User user2 = new User();
		user2.setId(2L);
		user2.setName("Bob Smith");
		user2.setEmail("bob@example.com");
		user2.setCreatedAt(Instant.now());

		org.mockito.Mockito.when(userService.listAll()).thenReturn(List.of(user1, user2));

		mockMvc.perform(get("/api/users"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json"))
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].name").value("Alice Johnson"))
				.andExpect(jsonPath("$[1].name").value("Bob Smith"));
	}
}
