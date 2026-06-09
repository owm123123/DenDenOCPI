package com.denden.memberauth.common.error;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ErrorResponse> handleApiException(ApiException exception) {
		ErrorCode errorCode = exception.getErrorCode();
		return ResponseEntity
			.status(errorCode.status())
			.body(new ErrorResponse(errorCode.name(), errorCode.message()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException() {
		ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
		return ResponseEntity
			.status(errorCode.status())
			.body(new ErrorResponse(errorCode.name(), errorCode.message()));
	}
}
