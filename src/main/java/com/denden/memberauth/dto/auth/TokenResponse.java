package com.denden.memberauth.dto.auth;

public record TokenResponse(
	String tokenType,
	String accessToken,
	long expiresIn,
	String refreshToken
) {
}
