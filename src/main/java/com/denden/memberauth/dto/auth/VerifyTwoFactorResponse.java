package com.denden.memberauth.dto.auth;

public record VerifyTwoFactorResponse(
	String tokenType,
	String accessToken,
	long expiresIn
) {
}
