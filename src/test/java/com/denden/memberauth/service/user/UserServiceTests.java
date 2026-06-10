package com.denden.memberauth.service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.denden.memberauth.common.error.ApiException;
import com.denden.memberauth.common.error.ErrorCode;
import com.denden.memberauth.dto.user.LastLoginResponse;
import com.denden.memberauth.entity.User;
import com.denden.memberauth.entity.UserStatus;
import com.denden.memberauth.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {

	private static final Instant NOW = Instant.parse("2026-06-09T00:00:00Z");

	@Mock
	private UserRepository userRepository;

	@Test
	@DisplayName("Should return current user's last login time")
	void shouldReturnCurrentUsersLastLoginTime() {
		User user = activeUser("member@example.com");
		user.markLoggedIn(NOW);
		UserService userService = new UserService(userRepository);
		when(userRepository.findByPublicId(user.getPublicId())).thenReturn(Optional.of(user));

		LastLoginResponse response = userService.getMyLastLogin(user.getPublicId().toString());

		assertThat(response).isEqualTo(new LastLoginResponse("member@example.com", NOW));
	}

	@Test
	@DisplayName("Should reject last login lookup when user does not exist")
	void shouldRejectLastLoginLookupWhenUserDoesNotExist() {
		UserService userService = new UserService(userRepository);
		UUID missingPublicId = UUID.fromString("11111111-1111-1111-1111-111111111111");
		when(userRepository.findByPublicId(missingPublicId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.getMyLastLogin(missingPublicId.toString()))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.USER_NOT_FOUND);
	}

	@Test
	@DisplayName("Should reject last login lookup when JWT subject is not a UUID")
	void shouldRejectLastLoginLookupWhenJwtSubjectIsNotUuid() {
		UserService userService = new UserService(userRepository);

		assertThatThrownBy(() -> userService.getMyLastLogin("member@example.com"))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.USER_NOT_FOUND);
	}

	private User activeUser(String email) {
		User user = new User(
			email,
			"encoded-password",
			UserStatus.PENDING_ACTIVATION,
			NOW.minus(Duration.ofHours(1)),
			NOW.minus(Duration.ofHours(1))
		);
		user.activate(NOW.minus(Duration.ofMinutes(30)));
		return user;
	}
}
