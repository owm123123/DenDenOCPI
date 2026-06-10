package com.denden.memberauth.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
	EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "Email is already registered."),
	ACCOUNT_NOT_ACTIVATED(HttpStatus.FORBIDDEN, "Account is not activated."),
	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Email or password is invalid."),
	INVALID_ACTIVATION_TOKEN(HttpStatus.BAD_REQUEST, "Activation token is invalid or expired."),
	INVALID_TWO_FACTOR_CODE(HttpStatus.BAD_REQUEST, "Two-factor verification code is invalid."),
	TWO_FACTOR_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "Two-factor verification code is expired."),
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User is not found."),
	VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Request validation failed."),
	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Request body is invalid."),
	UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Authentication is required or invalid."),
	TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Access token is expired."),
	INVALID_TOKEN_SIGNATURE(HttpStatus.UNAUTHORIZED, "Access token signature is invalid."),
	INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Access token is invalid."),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");

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
