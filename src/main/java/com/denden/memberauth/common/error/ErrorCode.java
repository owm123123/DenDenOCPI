package com.denden.memberauth.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
	EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "Email is already registered."),
	ACCOUNT_NOT_ACTIVATED(HttpStatus.FORBIDDEN, "Account is not activated."),
	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Email or password is invalid."),
	INVALID_ACTIVATION_TOKEN(HttpStatus.BAD_REQUEST, "Activation token is invalid or expired."),
	VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Request validation failed.");

	private final HttpStatus status;

	private final String message;

	ErrorCode(HttpStatus status, String message) {
		this.status = status;
		this.message = message;
	}

	public HttpStatus status() {
		return status;
	}

	public String message() {
		return message;
	}
}
