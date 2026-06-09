package com.denden.memberauth.service.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import org.springframework.stereotype.Service;

@Service
public class TwoFactorCodeService {

	private static final int CODE_BOUND = 1_000_000;

	private static final Duration EXPIRES_IN = Duration.ofMinutes(5);

	private final SecureRandom secureRandom = new SecureRandom();

	public TwoFactorCode generate() {
		String rawCode = "%06d".formatted(secureRandom.nextInt(CODE_BOUND));
		return new TwoFactorCode(rawCode, hash(rawCode), EXPIRES_IN);
	}

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

	public record TwoFactorCode(String rawCode, String codeHash, Duration expiresIn) {
	}
}
