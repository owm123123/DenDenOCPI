package com.denden.memberauth.service.auth;

import com.denden.memberauth.common.error.ApiException;
import com.denden.memberauth.common.error.ErrorCode;
import com.denden.memberauth.config.JwtProperties;
import com.denden.memberauth.entity.RefreshToken;
import com.denden.memberauth.entity.User;
import com.denden.memberauth.repository.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

	private static final int TOKEN_BYTES = 32;

	private final RefreshTokenRepository refreshTokenRepository;

	private final JwtProperties jwtProperties;

	private final Clock clock;

	private final SecureRandom secureRandom = new SecureRandom();

	public IssuedRefreshToken issue(User user) {
		Instant now = clock.instant();
		GeneratedToken generatedToken = generate();
		RefreshToken refreshToken = new RefreshToken(
			user,
			generatedToken.tokenHash(),
			now.plus(jwtProperties.refreshTokenExpiresIn()),
			now
		);
		refreshTokenRepository.save(refreshToken);
		return new IssuedRefreshToken(
			generatedToken.rawToken(),
			generatedToken.tokenHash(),
			refreshToken.getExpiresAt(),
			jwtProperties.refreshTokenExpiresIn().toSeconds()
		);
	}

	public RotatedRefreshToken rotate(String rawToken) {
		Instant now = clock.instant();
		RefreshToken currentToken = findUsableToken(rawToken, now);
		IssuedRefreshToken newToken = issue(currentToken.getUser());
		currentToken.replaceWith(newToken.tokenHash(), now);
		return new RotatedRefreshToken(currentToken.getUser(), newToken);
	}

	public void revoke(String rawToken) {
		Instant now = clock.instant();
		RefreshToken refreshToken = refreshTokenRepository
			.findByTokenHash(hash(rawToken))
			.orElseThrow(() -> new ApiException(ErrorCode.INVALID_REFRESH_TOKEN));

		if (!refreshToken.isRevoked()) {
			refreshToken.revoke(now);
		}
	}

	public String hash(String rawToken) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
		}
		catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available.", exception);
		}
	}

	private RefreshToken findUsableToken(String rawToken, Instant now) {
		RefreshToken refreshToken = refreshTokenRepository
			.findByTokenHash(hash(rawToken))
			.orElseThrow(() -> new ApiException(ErrorCode.INVALID_REFRESH_TOKEN));

		if (refreshToken.isRevoked()) {
			throw new ApiException(ErrorCode.INVALID_REFRESH_TOKEN);
		}

		if (refreshToken.isExpired(now)) {
			throw new ApiException(ErrorCode.REFRESH_TOKEN_EXPIRED);
		}

		return refreshToken;
	}

	private GeneratedToken generate() {
		byte[] bytes = new byte[TOKEN_BYTES];
		secureRandom.nextBytes(bytes);
		String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		return new GeneratedToken(rawToken, hash(rawToken));
	}

	private record GeneratedToken(String rawToken, String tokenHash) {
	}

	public record IssuedRefreshToken(String refreshToken, String tokenHash, Instant expiresAt, long expiresIn) {
	}

	public record RotatedRefreshToken(User user, IssuedRefreshToken refreshToken) {
	}
}
