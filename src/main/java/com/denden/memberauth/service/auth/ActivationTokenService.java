package com.denden.memberauth.service.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import org.springframework.stereotype.Service;

@Service
public class ActivationTokenService {

	private static final int TOKEN_BYTES = 32;

	private static final Duration EXPIRES_IN = Duration.ofHours(24);

	private final SecureRandom secureRandom = new SecureRandom();

	public ActivationToken generate() {
		byte[] bytes = new byte[TOKEN_BYTES];
		secureRandom.nextBytes(bytes);
		String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		return new ActivationToken(rawToken, hash(rawToken), EXPIRES_IN);
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

	public record ActivationToken(String rawToken, String tokenHash, Duration expiresIn) {
	}
}
