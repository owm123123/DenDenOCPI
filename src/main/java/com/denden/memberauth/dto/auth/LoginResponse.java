package com.denden.memberauth.dto.auth;

import java.time.Instant;

public record LoginResponse(
	String message,
	String challengeId,
	Instant expiresAt
) {
}
