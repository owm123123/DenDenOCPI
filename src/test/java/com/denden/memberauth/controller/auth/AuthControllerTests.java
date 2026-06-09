package com.denden.memberauth.controller.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.denden.memberauth.common.error.ApiException;
import com.denden.memberauth.common.error.ErrorCode;
import com.denden.memberauth.dto.auth.ActivateResponse;
import com.denden.memberauth.dto.auth.RegisterRequest;
import com.denden.memberauth.dto.auth.RegisterResponse;
import com.denden.memberauth.service.auth.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthService authService;

	@Test
	@DisplayName("Should return 201 when register request is valid")
	void shouldReturnCreatedWhenRegisterRequestIsValid() throws Exception {
		when(authService.register(any(RegisterRequest.class)))
			.thenReturn(new RegisterResponse("REGISTRATION_CREATED", "member@example.com"));

		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "email": "Member@Example.com",
					  "password": "P@ssw0rd123"
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.message").value("REGISTRATION_CREATED"))
			.andExpect(jsonPath("$.email").value("member@example.com"));
	}

	@Test
	@DisplayName("Should return validation error when register request is invalid")
	void shouldReturnValidationErrorWhenRegisterRequestIsInvalid() throws Exception {
		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "email": "not-an-email",
					  "password": "short"
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	@DisplayName("Should map duplicate email error to 409")
	void shouldMapDuplicateEmailErrorToConflict() throws Exception {
		when(authService.register(any(RegisterRequest.class)))
			.thenThrow(new ApiException(ErrorCode.EMAIL_ALREADY_REGISTERED));

		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "email": "member@example.com",
					  "password": "P@ssw0rd123"
					}
					"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
	}

	@Test
	@DisplayName("Should return 200 when activation token is valid")
	void shouldReturnOkWhenActivationTokenIsValid() throws Exception {
		when(authService.activate("activation-token"))
			.thenReturn(new ActivateResponse("ACCOUNT_ACTIVATED"));

		mockMvc.perform(get("/api/auth/activate")
				.param("token", "activation-token"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("ACCOUNT_ACTIVATED"));
	}

	@Test
	@DisplayName("Should map invalid activation token error to 400")
	void shouldMapInvalidActivationTokenErrorToBadRequest() throws Exception {
		when(authService.activate("invalid-token"))
			.thenThrow(new ApiException(ErrorCode.INVALID_ACTIVATION_TOKEN));

		mockMvc.perform(get("/api/auth/activate")
				.param("token", "invalid-token"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_ACTIVATION_TOKEN"));
	}
}
