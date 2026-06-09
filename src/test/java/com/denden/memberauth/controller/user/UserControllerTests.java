package com.denden.memberauth.controller.user;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.denden.memberauth.common.error.ApiException;
import com.denden.memberauth.common.error.ErrorCode;
import com.denden.memberauth.dto.user.LastLoginResponse;
import com.denden.memberauth.service.user.UserService;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
class UserControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	@Test
	@DisplayName("Should return current user's last login time")
	void shouldReturnCurrentUsersLastLoginTime() throws Exception {
		when(userService.getMyLastLogin("member@example.com"))
			.thenReturn(new LastLoginResponse("member@example.com", Instant.parse("2026-06-09T08:15:00Z")));

		mockMvc.perform(get("/api/users/last-login")
				.with(jwt().jwt(jwt -> jwt.subject("member@example.com"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("member@example.com"))
			.andExpect(jsonPath("$.lastLoginAt").value("2026-06-09T08:15:00Z"));
	}

	@Test
	@DisplayName("Should map missing current user to 404")
	void shouldMapMissingCurrentUserToNotFound() throws Exception {
		when(userService.getMyLastLogin("missing@example.com"))
			.thenThrow(new ApiException(ErrorCode.USER_NOT_FOUND));

		mockMvc.perform(get("/api/users/last-login")
				.with(jwt().jwt(jwt -> jwt.subject("missing@example.com"))))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
	}

	@Test
	@DisplayName("Should reject last login lookup without JWT")
	void shouldRejectLastLoginLookupWithoutJwt() throws Exception {
		mockMvc.perform(get("/api/users/last-login"))
			.andExpect(status().isUnauthorized());
	}
}
