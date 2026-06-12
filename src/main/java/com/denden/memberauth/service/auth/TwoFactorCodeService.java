package com.denden.memberauth.service.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import org.springframework.stereotype.Service;

/**
 * 負責產生與雜湊登入用 Email 二階段驗證碼。
 *
 * <p>驗證碼只透過 Email 傳給使用者，資料庫保存 hash；登入完成前不會簽發 JWT。</p>
 */
@Service
public class TwoFactorCodeService {

	private static final int CODE_BOUND = 1_000_000;

	private static final Duration EXPIRES_IN = Duration.ofMinutes(5);

	private final SecureRandom secureRandom = new SecureRandom();

	/**
	 * 產生 6 位數二階段驗證碼與對應 hash。
	 */
	public TwoFactorCode generate() {
		String rawCode = "%06d".formatted(secureRandom.nextInt(CODE_BOUND));
		return new TwoFactorCode(rawCode, hash(rawCode), EXPIRES_IN);
	}

	/**
	 * 將明文二階段驗證碼轉成資料庫保存與比對用的 hash。
	 */
	public String hash(String rawCode) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(rawCode.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
		}
		catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available.", exception);
		}
	}

	/**
	 * Email 二階段驗證碼的產生結果。
	 *
	 * @param rawCode 明文 6 位數驗證碼，會寄給使用者
	 * @param codeHash 資料庫保存與比對使用的驗證碼 hash
	 * @param expiresIn 驗證碼有效時間
	 */
	public record TwoFactorCode(String rawCode, String codeHash, Duration expiresIn) {
	}
}
