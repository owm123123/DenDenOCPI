package com.denden.memberauth.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.denden.memberauth.common.error.ApiException;
import com.denden.memberauth.common.error.ErrorCode;
import com.denden.memberauth.dto.auth.ActivateResponse;
import com.denden.memberauth.dto.auth.RegisterRequest;
import com.denden.memberauth.dto.auth.RegisterResponse;
import com.denden.memberauth.email.ActivationEmailSender;
import com.denden.memberauth.entity.EmailActivationToken;
import com.denden.memberauth.entity.User;
import com.denden.memberauth.entity.UserStatus;
import com.denden.memberauth.repository.EmailActivationTokenRepository;
import com.denden.memberauth.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

	private static final Instant NOW = Instant.parse("2026-06-09T00:00:00Z");

	@Mock
	private UserRepository userRepository;

	@Mock
	private EmailActivationTokenRepository emailActivationTokenRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private ActivationTokenService activationTokenService;

	@Mock
	private ActivationEmailSender activationEmailSender;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(
			userRepository,
			emailActivationTokenRepository,
			passwordEncoder,
			activationTokenService,
			activationEmailSender,
			Clock.fixed(NOW, ZoneOffset.UTC)
		);
	}

	@Test
	@DisplayName("Should register pending user and send activation email")
	void shouldRegisterPendingUserAndSendActivationEmail() {
		when(userRepository.existsByEmail("member@example.com")).thenReturn(false);
		when(passwordEncoder.encode("P@ssw0rd123")).thenReturn("encoded-password");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(activationTokenService.generate())
			.thenReturn(new ActivationTokenService.ActivationToken("raw-token", "token-hash", Duration.ofHours(24)));

		RegisterResponse response = authService.register(new RegisterRequest(" Member@Example.com ", "P@ssw0rd123"));

		assertThat(response).isEqualTo(new RegisterResponse("REGISTRATION_CREATED", "member@example.com"));

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(userCaptor.capture());
		assertThat(userCaptor.getValue().getEmail()).isEqualTo("member@example.com");
		assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("encoded-password");
		assertThat(userCaptor.getValue().getStatus()).isEqualTo(UserStatus.PENDING_ACTIVATION);

		ArgumentCaptor<EmailActivationToken> tokenCaptor = ArgumentCaptor.forClass(EmailActivationToken.class);
		verify(emailActivationTokenRepository).save(tokenCaptor.capture());
		assertThat(tokenCaptor.getValue().getTokenHash()).isEqualTo("token-hash");
		assertThat(tokenCaptor.getValue().getExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(24)));

		verify(activationEmailSender).sendActivationEmail("member@example.com", "raw-token");
	}

	@Test
	@DisplayName("Should reject duplicate email registration")
	void shouldRejectDuplicateEmailRegistration() {
		when(userRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

		assertThatThrownBy(() -> authService.register(new RegisterRequest("duplicate@example.com", "P@ssw0rd123")))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.EMAIL_ALREADY_REGISTERED);

		verifyNoInteractions(emailActivationTokenRepository, activationEmailSender);
	}

	@Test
	@DisplayName("Should activate user with valid token")
	void shouldActivateUserWithValidToken() {
		User user = pendingUser("activate@example.com");
		EmailActivationToken token = new EmailActivationToken(
			user,
			"token-hash",
			NOW.plus(Duration.ofHours(1)),
			NOW.minus(Duration.ofHours(1))
		);
		when(activationTokenService.hash("raw-token")).thenReturn("token-hash");
		when(emailActivationTokenRepository.findByTokenHash("token-hash")).thenReturn(Optional.of(token));

		ActivateResponse response = authService.activate("raw-token");

		assertThat(response).isEqualTo(new ActivateResponse("ACCOUNT_ACTIVATED"));
		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(user.getActivatedAt()).isEqualTo(NOW);
		assertThat(token.getUsedAt()).isEqualTo(NOW);
	}

	@Test
	@DisplayName("Should reject missing activation token")
	void shouldRejectMissingActivationToken() {
		when(activationTokenService.hash("missing-token")).thenReturn("missing-hash");
		when(emailActivationTokenRepository.findByTokenHash("missing-hash")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.activate("missing-token"))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.INVALID_ACTIVATION_TOKEN);
	}

	@Test
	@DisplayName("Should reject expired activation token")
	void shouldRejectExpiredActivationToken() {
		User user = pendingUser("expired@example.com");
		EmailActivationToken token = new EmailActivationToken(
			user,
			"expired-hash",
			NOW,
			NOW.minus(Duration.ofDays(1))
		);
		when(activationTokenService.hash("expired-token")).thenReturn("expired-hash");
		when(emailActivationTokenRepository.findByTokenHash("expired-hash")).thenReturn(Optional.of(token));

		assertThatThrownBy(() -> authService.activate("expired-token"))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.INVALID_ACTIVATION_TOKEN);

		assertThat(user.getStatus()).isEqualTo(UserStatus.PENDING_ACTIVATION);
		assertThat(token.getUsedAt()).isNull();
	}

	@Test
	@DisplayName("Should reject used activation token")
	void shouldRejectUsedActivationToken() {
		User user = pendingUser("used@example.com");
		EmailActivationToken token = new EmailActivationToken(
			user,
			"used-hash",
			NOW.plus(Duration.ofHours(1)),
			NOW.minus(Duration.ofHours(1))
		);
		token.markUsed(NOW.minus(Duration.ofMinutes(5)));
		when(activationTokenService.hash("used-token")).thenReturn("used-hash");
		when(emailActivationTokenRepository.findByTokenHash("used-hash")).thenReturn(Optional.of(token));

		assertThatThrownBy(() -> authService.activate("used-token"))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.INVALID_ACTIVATION_TOKEN);
	}

	private User pendingUser(String email) {
		return new User(
			email,
			"encoded-password",
			UserStatus.PENDING_ACTIVATION,
			NOW.minus(Duration.ofHours(1)),
			NOW.minus(Duration.ofHours(1))
		);
	}
}
