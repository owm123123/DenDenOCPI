package com.denden.memberauth.dto.auth;

import java.time.Instant;

public record VerifyTwoFactorResponse(
	String message,
	String email,
	Instant lastLoginAt
) {
}
