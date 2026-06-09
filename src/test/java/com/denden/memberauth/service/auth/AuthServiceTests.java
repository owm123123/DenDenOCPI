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
import com.denden.memberauth.dto.auth.LoginRequest;
import com.denden.memberauth.dto.auth.LoginResponse;
import com.denden.memberauth.dto.auth.RegisterRequest;
import com.denden.memberauth.dto.auth.RegisterResponse;
import com.denden.memberauth.dto.auth.VerifyTwoFactorRequest;
import com.denden.memberauth.dto.auth.VerifyTwoFactorResponse;
import com.denden.memberauth.email.AuthEmailSender;
import com.denden.memberauth.entity.EmailActivationToken;
import com.denden.memberauth.entity.LoginTwoFactorCode;
import com.denden.memberauth.entity.User;
import com.denden.memberauth.entity.UserStatus;
import com.denden.memberauth.repository.EmailActivationTokenRepository;
import com.denden.memberauth.repository.LoginTwoFactorCodeRepository;
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
	private LoginTwoFactorCodeRepository loginTwoFactorCodeRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private ActivationTokenService activationTokenService;

	@Mock
	private TwoFactorCodeService twoFactorCodeService;

	@Mock
	private AuthEmailSender authEmailSender;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(
			userRepository,
			emailActivationTokenRepository,
			loginTwoFactorCodeRepository,
			passwordEncoder,
			activationTokenService,
			twoFactorCodeService,
			authEmailSender,
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

		verify(authEmailSender).sendActivationEmail("member@example.com", "raw-token");
	}

	@Test
	@DisplayName("Should reject duplicate email registration")
	void shouldRejectDuplicateEmailRegistration() {
		when(userRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

		assertThatThrownBy(() -> authService.register(new RegisterRequest("duplicate@example.com", "P@ssw0rd123")))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.EMAIL_ALREADY_REGISTERED);

		verifyNoInteractions(emailActivationTokenRepository, authEmailSender);
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

	@Test
	@DisplayName("Should create two-factor challenge when login credentials are valid")
	void shouldCreateTwoFactorChallengeWhenLoginCredentialsAreValid() {
		User user = activeUser("member@example.com");
		when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("P@ssw0rd123", "encoded-password")).thenReturn(true);
		when(twoFactorCodeService.generate())
			.thenReturn(new TwoFactorCodeService.TwoFactorCode("123456", "code-hash", Duration.ofMinutes(5)));
		when(loginTwoFactorCodeRepository.save(any(LoginTwoFactorCode.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		LoginResponse response = authService.login(new LoginRequest(" Member@Example.com ", "P@ssw0rd123"));

		assertThat(response.message()).isEqualTo("TWO_FACTOR_REQUIRED");
		assertThat(response.challengeId()).isNotBlank();
		assertThat(response.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(5)));

		ArgumentCaptor<LoginTwoFactorCode> codeCaptor = ArgumentCaptor.forClass(LoginTwoFactorCode.class);
		verify(loginTwoFactorCodeRepository).save(codeCaptor.capture());
		assertThat(codeCaptor.getValue().getUser()).isEqualTo(user);
		assertThat(codeCaptor.getValue().getChallengeId()).isEqualTo(response.challengeId());
		assertThat(codeCaptor.getValue().getCodeHash()).isEqualTo("code-hash");
		assertThat(codeCaptor.getValue().getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(5)));

		verify(authEmailSender).sendTwoFactorCode("member@example.com", "123456");
	}

	@Test
	@DisplayName("Should reject login when password is invalid")
	void shouldRejectLoginWhenPasswordIsInvalid() {
		User user = activeUser("member@example.com");
		when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

		assertThatThrownBy(() -> authService.login(new LoginRequest("member@example.com", "wrong-password")))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.INVALID_CREDENTIALS);

		verifyNoInteractions(loginTwoFactorCodeRepository, authEmailSender);
	}

	@Test
	@DisplayName("Should reject login when email does not exist")
	void shouldRejectLoginWhenEmailDoesNotExist() {
		when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login(new LoginRequest("missing@example.com", "P@ssw0rd123")))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.INVALID_CREDENTIALS);

		verifyNoInteractions(loginTwoFactorCodeRepository, authEmailSender);
	}

	@Test
	@DisplayName("Should reject login when account is not activated")
	void shouldRejectLoginWhenAccountIsNotActivated() {
		User user = pendingUser("pending@example.com");
		when(userRepository.findByEmail("pending@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("P@ssw0rd123", "encoded-password")).thenReturn(true);

		assertThatThrownBy(() -> authService.login(new LoginRequest("pending@example.com", "P@ssw0rd123")))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.ACCOUNT_NOT_ACTIVATED);

		verifyNoInteractions(loginTwoFactorCodeRepository, authEmailSender);
	}

	@Test
	@DisplayName("Should verify two-factor code and update last login time")
	void shouldVerifyTwoFactorCodeAndUpdateLastLoginTime() {
		User user = activeUser("member@example.com");
		LoginTwoFactorCode challenge = twoFactorChallenge(user, "challenge-id", "code-hash", NOW.plus(Duration.ofMinutes(5)));
		when(loginTwoFactorCodeRepository.findByChallengeId("challenge-id")).thenReturn(Optional.of(challenge));
		when(twoFactorCodeService.hash("123456")).thenReturn("code-hash");

		VerifyTwoFactorResponse response = authService.verifyTwoFactor(
			new VerifyTwoFactorRequest("challenge-id", "123456")
		);

		assertThat(response).isEqualTo(new VerifyTwoFactorResponse("TWO_FACTOR_VERIFIED", "member@example.com", NOW));
		assertThat(challenge.getVerifiedAt()).isEqualTo(NOW);
		assertThat(user.getLastLoginAt()).isEqualTo(NOW);
	}

	@Test
	@DisplayName("Should reject two-factor verification when challenge is missing")
	void shouldRejectTwoFactorVerificationWhenChallengeIsMissing() {
		when(loginTwoFactorCodeRepository.findByChallengeId("missing-challenge")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.verifyTwoFactor(
				new VerifyTwoFactorRequest("missing-challenge", "123456")
			))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.INVALID_TWO_FACTOR_CODE);
	}

	@Test
	@DisplayName("Should reject expired two-factor code")
	void shouldRejectExpiredTwoFactorCode() {
		User user = activeUser("expired@example.com");
		LoginTwoFactorCode challenge = twoFactorChallenge(user, "expired-challenge", "code-hash", NOW);
		when(loginTwoFactorCodeRepository.findByChallengeId("expired-challenge")).thenReturn(Optional.of(challenge));

		assertThatThrownBy(() -> authService.verifyTwoFactor(
				new VerifyTwoFactorRequest("expired-challenge", "123456")
			))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.TWO_FACTOR_CODE_EXPIRED);

		assertThat(challenge.getVerifiedAt()).isNull();
		assertThat(user.getLastLoginAt()).isNull();
	}

	@Test
	@DisplayName("Should reject already verified two-factor challenge")
	void shouldRejectAlreadyVerifiedTwoFactorChallenge() {
		User user = activeUser("verified@example.com");
		LoginTwoFactorCode challenge = twoFactorChallenge(user, "verified-challenge", "code-hash", NOW.plus(Duration.ofMinutes(5)));
		challenge.markVerified(NOW.minus(Duration.ofMinutes(1)));
		when(loginTwoFactorCodeRepository.findByChallengeId("verified-challenge")).thenReturn(Optional.of(challenge));

		assertThatThrownBy(() -> authService.verifyTwoFactor(
				new VerifyTwoFactorRequest("verified-challenge", "123456")
			))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.INVALID_TWO_FACTOR_CODE);
	}

	@Test
	@DisplayName("Should reject invalid two-factor code and increase failed attempts")
	void shouldRejectInvalidTwoFactorCodeAndIncreaseFailedAttempts() {
		User user = activeUser("invalid@example.com");
		LoginTwoFactorCode challenge = twoFactorChallenge(user, "invalid-challenge", "code-hash", NOW.plus(Duration.ofMinutes(5)));
		when(loginTwoFactorCodeRepository.findByChallengeId("invalid-challenge")).thenReturn(Optional.of(challenge));
		when(twoFactorCodeService.hash("000000")).thenReturn("wrong-code-hash");

		assertThatThrownBy(() -> authService.verifyTwoFactor(
				new VerifyTwoFactorRequest("invalid-challenge", "000000")
			))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.INVALID_TWO_FACTOR_CODE);

		assertThat(challenge.getFailedAttempts()).isEqualTo(1);
		assertThat(challenge.getVerifiedAt()).isNull();
		assertThat(user.getLastLoginAt()).isNull();
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

	private User activeUser(String email) {
		User user = pendingUser(email);
		user.activate(NOW.minus(Duration.ofMinutes(30)));
		return user;
	}

	private LoginTwoFactorCode twoFactorChallenge(User user, String challengeId, String codeHash, Instant expiresAt) {
		return new LoginTwoFactorCode(
			user,
			challengeId,
			codeHash,
			expiresAt,
			NOW.minus(Duration.ofMinutes(1))
		);
	}
}
