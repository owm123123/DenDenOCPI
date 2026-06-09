package com.denden.memberauth.dto.user;

import java.time.Instant;

public record LastLoginResponse(
	String email,
	Instant lastLoginAt
) {
}
