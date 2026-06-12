package com.denden.memberauth.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.denden.memberauth.common.error.ApiException;
import com.denden.memberauth.common.error.ErrorCode;
import com.denden.memberauth.config.JwtProperties;
import com.denden.memberauth.entity.RefreshToken;
import com.denden.memberauth.entity.User;
import com.denden.memberauth.entity.UserStatus;
import com.denden.memberauth.repository.RefreshTokenRepository;
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

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTests {

	private static final Instant NOW = Instant.parse("2026-06-09T00:00:00Z");

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	private RefreshTokenService refreshTokenService;

	@BeforeEach
	void setUp() {
		refreshTokenService = new RefreshTokenService(
			refreshTokenRepository,
			new JwtProperties("http://localhost:8080", "test-secret-change-me-32-byte-key", Duration.ofHours(1), Duration.ofDays(7)),
			Clock.fixed(NOW, ZoneOffset.UTC)
		);
	}

	@Test
	@DisplayName("Should issue refresh token and persist only token hash")
	void shouldIssueRefreshTokenAndPersistOnlyTokenHash() {
		User user = activeUser();
		when(refreshTokenRepository.save(any(RefreshToken.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		RefreshTokenService.IssuedRefreshToken issuedToken = refreshTokenService.issue(user);

		assertThat(issuedToken.refreshToken()).isNotBlank();
		assertThat(issuedToken.tokenHash()).isNotBlank();
		assertThat(issuedToken.refreshToken()).isNotEqualTo(issuedToken.tokenHash());
		assertThat(issuedToken.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
		assertThat(issuedToken.expiresIn()).isEqualTo(604800);

		ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokenRepository).save(tokenCaptor.capture());
		assertThat(tokenCaptor.getValue().getUser()).isEqualTo(user);
		assertThat(tokenCaptor.getValue().getTokenHash()).isEqualTo(issuedToken.tokenHash());
		assertThat(tokenCaptor.getValue().getExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
		assertThat(tokenCaptor.getValue().getCreatedAt()).isEqualTo(NOW);
	}

	@Test
	@DisplayName("Should rotate refresh token and revoke previous token")
	void shouldRotateRefreshTokenAndRevokePreviousToken() {
		User user = activeUser();
		String oldRefreshToken = "old-refresh-token";
		RefreshToken currentToken = new RefreshToken(
			user,
			refreshTokenService.hash(oldRefreshToken),
			NOW.plus(Duration.ofDays(1)),
			NOW.minus(Duration.ofDays(1))
		);
		when(refreshTokenRepository.findByTokenHash(refreshTokenService.hash(oldRefreshToken)))
			.thenReturn(Optional.of(currentToken));
		when(refreshTokenRepository.save(any(RefreshToken.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		RefreshTokenService.RotatedRefreshToken rotatedToken = refreshTokenService.rotate(oldRefreshToken);

		assertThat(rotatedToken.user()).isEqualTo(user);
		assertThat(rotatedToken.refreshToken().refreshToken()).isNotBlank();
		assertThat(currentToken.getRevokedAt()).isEqualTo(NOW);
		assertThat(currentToken.getReplacedByTokenHash()).isEqualTo(rotatedToken.refreshToken().tokenHash());
	}

	@Test
	@DisplayName("Should reject missing refresh token")
	void shouldRejectMissingRefreshToken() {
		when(refreshTokenRepository.findByTokenHash(refreshTokenService.hash("missing-refresh-token")))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> refreshTokenService.rotate("missing-refresh-token"))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
	}

	@Test
	@DisplayName("Should reject revoked refresh token")
	void shouldRejectRevokedRefreshToken() {
		User user = activeUser();
		String rawToken = "revoked-refresh-token";
		RefreshToken refreshToken = new RefreshToken(
			user,
			refreshTokenService.hash(rawToken),
			NOW.plus(Duration.ofDays(1)),
			NOW.minus(Duration.ofDays(1))
		);
		refreshToken.revoke(NOW.minus(Duration.ofMinutes(1)));
		when(refreshTokenRepository.findByTokenHash(refreshTokenService.hash(rawToken)))
			.thenReturn(Optional.of(refreshToken));

		assertThatThrownBy(() -> refreshTokenService.rotate(rawToken))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
	}

	@Test
	@DisplayName("Should reject expired refresh token")
	void shouldRejectExpiredRefreshToken() {
		User user = activeUser();
		String rawToken = "expired-refresh-token";
		RefreshToken refreshToken = new RefreshToken(
			user,
			refreshTokenService.hash(rawToken),
			NOW,
			NOW.minus(Duration.ofDays(7))
		);
		when(refreshTokenRepository.findByTokenHash(refreshTokenService.hash(rawToken)))
			.thenReturn(Optional.of(refreshToken));

		assertThatThrownBy(() -> refreshTokenService.rotate(rawToken))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.REFRESH_TOKEN_EXPIRED);
	}

	@Test
	@DisplayName("Should revoke refresh token")
	void shouldRevokeRefreshToken() {
		User user = activeUser();
		String rawToken = "refresh-token";
		RefreshToken refreshToken = new RefreshToken(
			user,
			refreshTokenService.hash(rawToken),
			NOW.plus(Duration.ofDays(1)),
			NOW.minus(Duration.ofDays(1))
		);
		when(refreshTokenRepository.findByTokenHash(refreshTokenService.hash(rawToken)))
			.thenReturn(Optional.of(refreshToken));

		refreshTokenService.revoke(rawToken);

		assertThat(refreshToken.getRevokedAt()).isEqualTo(NOW);
	}

	private User activeUser() {
		User user = new User(
			"member@example.com",
			"encoded-password",
			UserStatus.PENDING_ACTIVATION,
			NOW.minus(Duration.ofDays(1)),
			NOW.minus(Duration.ofDays(1))
		);
		user.activate(NOW.minus(Duration.ofHours(1)));
		return user;
	}
}
