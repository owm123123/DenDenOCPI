package com.denden.memberauth.service.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import org.springframework.stereotype.Service;

/**
 * 負責產生與雜湊 Email 開通信使用的 activation token。
 *
 * <p>明文 token 只放在 Email 連結中交給使用者，資料庫保存 hash，避免直接保存可開通帳號的敏感值。</p>
 */
@Service
public class ActivationTokenService {

	private static final int TOKEN_BYTES = 32;

	private static final Duration EXPIRES_IN = Duration.ofHours(24);

	private final SecureRandom secureRandom = new SecureRandom();

	/**
	 * 產生一組高熵 activation token 與對應 hash。
	 */
	public ActivationToken generate() {
		byte[] bytes = new byte[TOKEN_BYTES];
		secureRandom.nextBytes(bytes);
		String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		return new ActivationToken(rawToken, hash(rawToken), EXPIRES_IN);
	}

	/**
	 * 將明文 activation token 轉成資料庫保存與查詢用的 hash。
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

	/**
	 * Email 開通信 token 的產生結果。
	 *
	 * @param rawToken 明文 token，會放入開通信連結
	 * @param tokenHash 資料庫保存與比對使用的 token hash
	 * @param expiresIn token 有效時間
	 */
	public record ActivationToken(String rawToken, String tokenHash, Duration expiresIn) {
	}
}
