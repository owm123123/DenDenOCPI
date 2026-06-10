package com.denden.memberauth.common.error;

import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ErrorResponse> handleApiException(ApiException exception) {
		ErrorCode errorCode = exception.getErrorCode();
		return ResponseEntity
			.status(errorCode.status())
			.body(new ErrorResponse(errorCode.name(), errorCode.message()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
		ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
		List<ErrorResponse.FieldError> fieldErrors = exception.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(fieldError -> new ErrorResponse.FieldError(fieldError.getField(), fieldError.getDefaultMessage()))
			.sorted(Comparator.comparing(ErrorResponse.FieldError::field))
			.toList();
		return ResponseEntity
			.status(errorCode.status())
			.body(new ErrorResponse(errorCode.name(), errorCode.message(), fieldErrors));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleInvalidRequestBody() {
		ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
		return ResponseEntity
			.status(errorCode.status())
			.body(new ErrorResponse(errorCode.name(), errorCode.message()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception exception) {
		log.error("Unexpected API error", exception);
		ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
		return ResponseEntity
			.status(errorCode.status())
			.body(new ErrorResponse(errorCode.name(), errorCode.message()));
	}
}
