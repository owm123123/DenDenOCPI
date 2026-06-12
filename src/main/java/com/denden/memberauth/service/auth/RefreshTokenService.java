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

/**
 * 負責 refresh token 的產生、雜湊、輪替與撤銷。
 *
 * <p>refresh token 明文只會回傳給 client 一次，資料庫保存的是 SHA-256 hash。
 * 這讓 server 可以撤銷 refresh token，同時避免資料庫外洩時直接取得可用 token。</p>
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

	private static final int TOKEN_BYTES = 32;

	private final RefreshTokenRepository refreshTokenRepository;

	private final JwtProperties jwtProperties;

	private final Clock clock;

	private final SecureRandom secureRandom = new SecureRandom();

	/**
	 * 為指定使用者簽發新的 refresh token。
	 *
	 * <p>回傳值包含明文 token 給 client 使用，也包含內部需要的 token hash 與過期資訊。</p>
	 */
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

	/**
	 * 驗證並輪替 refresh token。
	 *
	 * <p>舊 token 必須存在、未撤銷且未過期；成功後會建立新 token，並把舊 token 標記為已被新 token 取代。</p>
	 */
	public RotatedRefreshToken rotate(String rawToken) {
		Instant now = clock.instant();
		RefreshToken currentToken = findUsableToken(rawToken, now);
		IssuedRefreshToken newToken = issue(currentToken.getUser());
		currentToken.replaceWith(newToken.tokenHash(), now);
		return new RotatedRefreshToken(currentToken.getUser(), newToken);
	}

	/**
	 * 撤銷指定 refresh token。
	 *
	 * <p>用於登出流程；已撤銷的 token 不能再用來呼叫 refresh API。</p>
	 */
	public void revoke(String rawToken) {
		Instant now = clock.instant();
		RefreshToken refreshToken = refreshTokenRepository
			.findByTokenHash(hash(rawToken))
			.orElseThrow(() -> new ApiException(ErrorCode.INVALID_REFRESH_TOKEN));

		if (!refreshToken.isRevoked()) {
			refreshToken.revoke(now);
		}
	}

	/**
	 * 將明文 refresh token 轉成資料庫保存用的 hash。
	 */
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

	/**
	 * 新簽發的 refresh token 資訊。
	 *
	 * @param refreshToken 明文 refresh token，只回傳給 client
	 * @param tokenHash 資料庫保存與比對使用的 token hash
	 * @param expiresAt refresh token 的 UTC 過期時間
	 * @param expiresIn refresh token 有效秒數
	 */
	public record IssuedRefreshToken(String refreshToken, String tokenHash, Instant expiresAt, long expiresIn) {
	}

	/**
	 * refresh token rotation 成功後的結果。
	 *
	 * @param user token 所屬使用者
	 * @param refreshToken 新簽發的 refresh token
	 */
	public record RotatedRefreshToken(User user, IssuedRefreshToken refreshToken) {
	}
}
