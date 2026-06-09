package com.denden.memberauth.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VerifyTwoFactorRequest(
	@NotBlank
	@Size(max = 36)
	String challengeId,

	@NotBlank
	@Pattern(regexp = "\\d{6}")
	String code
) {
}
