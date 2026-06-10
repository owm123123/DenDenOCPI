package com.denden.memberauth.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(String code, String message, List<FieldError> fieldErrors) {

	public ErrorResponse(String code, String message) {
		this(code, message, List.of());
	}

	public record FieldError(String field, String message) {
	}
}
