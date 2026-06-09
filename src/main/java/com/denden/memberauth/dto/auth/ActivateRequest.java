package com.denden.memberauth.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record ActivateRequest(
	@NotBlank
	String activationToken
) {
}
