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
import com.denden.memberauth.dto.auth.LoginRequest;
import com.denden.memberauth.dto.auth.LoginResponse;
import com.denden.memberauth.dto.auth.RegisterRequest;
import com.denden.memberauth.dto.auth.RegisterResponse;
import com.denden.memberauth.dto.auth.VerifyTwoFactorRequest;
import com.denden.memberauth.dto.auth.VerifyTwoFactorResponse;
import com.denden.memberauth.service.auth.AuthService;
import java.time.Instant;
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

	@Test
	@DisplayName("Should return 200 when login credentials are valid")
	void shouldReturnOkWhenLoginCredentialsAreValid() throws Exception {
		when(authService.login(any(LoginRequest.class)))
			.thenReturn(new LoginResponse(
				"TWO_FACTOR_REQUIRED",
				"22222222-2222-2222-2222-222222222222",
				Instant.parse("2026-06-09T08:10:00Z")
			));

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "email": "member@example.com",
					  "password": "P@ssw0rd123"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("TWO_FACTOR_REQUIRED"))
			.andExpect(jsonPath("$.challengeId").value("22222222-2222-2222-2222-222222222222"))
			.andExpect(jsonPath("$.expiresAt").value("2026-06-09T08:10:00Z"));
	}

	@Test
	@DisplayName("Should return validation error when login request is invalid")
	void shouldReturnValidationErrorWhenLoginRequestIsInvalid() throws Exception {
		mockMvc.perform(post("/api/auth/login")
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
	@DisplayName("Should map invalid credentials error to 401")
	void shouldMapInvalidCredentialsErrorToUnauthorized() throws Exception {
		when(authService.login(any(LoginRequest.class)))
			.thenThrow(new ApiException(ErrorCode.INVALID_CREDENTIALS));

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "email": "member@example.com",
					  "password": "wrong-password"
					}
					"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
	}

	@Test
	@DisplayName("Should map not activated account error to 403")
	void shouldMapNotActivatedAccountErrorToForbidden() throws Exception {
		when(authService.login(any(LoginRequest.class)))
			.thenThrow(new ApiException(ErrorCode.ACCOUNT_NOT_ACTIVATED));

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "email": "pending@example.com",
					  "password": "P@ssw0rd123"
					}
					"""))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("ACCOUNT_NOT_ACTIVATED"));
	}

	@Test
	@DisplayName("Should return 200 when two-factor code is valid")
	void shouldReturnOkWhenTwoFactorCodeIsValid() throws Exception {
		when(authService.verifyTwoFactor(any(VerifyTwoFactorRequest.class)))
			.thenReturn(new VerifyTwoFactorResponse(
				"TWO_FACTOR_VERIFIED",
				"member@example.com",
				Instant.parse("2026-06-09T08:15:00Z")
			));

		mockMvc.perform(post("/api/auth/2fa/verify")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "challengeId": "22222222-2222-2222-2222-222222222222",
					  "code": "123456"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("TWO_FACTOR_VERIFIED"))
			.andExpect(jsonPath("$.email").value("member@example.com"))
			.andExpect(jsonPath("$.lastLoginAt").value("2026-06-09T08:15:00Z"));
	}

	@Test
	@DisplayName("Should return validation error when two-factor request is invalid")
	void shouldReturnValidationErrorWhenTwoFactorRequestIsInvalid() throws Exception {
		mockMvc.perform(post("/api/auth/2fa/verify")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "challengeId": "",
					  "code": "abc"
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	@DisplayName("Should map invalid two-factor code error to 400")
	void shouldMapInvalidTwoFactorCodeErrorToBadRequest() throws Exception {
		when(authService.verifyTwoFactor(any(VerifyTwoFactorRequest.class)))
			.thenThrow(new ApiException(ErrorCode.INVALID_TWO_FACTOR_CODE));

		mockMvc.perform(post("/api/auth/2fa/verify")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "challengeId": "22222222-2222-2222-2222-222222222222",
					  "code": "000000"
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_TWO_FACTOR_CODE"));
	}

	@Test
	@DisplayName("Should map expired two-factor code error to 400")
	void shouldMapExpiredTwoFactorCodeErrorToBadRequest() throws Exception {
		when(authService.verifyTwoFactor(any(VerifyTwoFactorRequest.class)))
			.thenThrow(new ApiException(ErrorCode.TWO_FACTOR_CODE_EXPIRED));

		mockMvc.perform(post("/api/auth/2fa/verify")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "challengeId": "22222222-2222-2222-2222-222222222222",
					  "code": "123456"
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("TWO_FACTOR_CODE_EXPIRED"));
	}
}
