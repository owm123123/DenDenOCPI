package com.denden.memberauth.controller.auth;

import com.denden.memberauth.dto.auth.ActivateRequest;
import com.denden.memberauth.dto.auth.ActivateResponse;
import com.denden.memberauth.dto.auth.LoginRequest;
import com.denden.memberauth.dto.auth.LoginResponse;
import com.denden.memberauth.dto.auth.LogoutRequest;
import com.denden.memberauth.dto.auth.RefreshTokenRequest;
import com.denden.memberauth.dto.auth.RegisterRequest;
import com.denden.memberauth.dto.auth.RegisterResponse;
import com.denden.memberauth.dto.auth.TokenResponse;
import com.denden.memberauth.dto.auth.VerifyTwoFactorRequest;
import com.denden.memberauth.dto.auth.VerifyTwoFactorResponse;
import com.denden.memberauth.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registration, email activation, login, and email 2FA APIs")
public class AuthController {

	private final AuthService authService;

	@PostMapping("/register")
	@Operation(summary = "Register a pending member and send activation email")
	public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
		RegisterResponse response = authService.register(request);
		return ResponseEntity
			.status(HttpStatus.CREATED)
			.body(response);
	}

	@PostMapping("/activate")
	@Operation(summary = "Activate a member account with an email activation token")
	public ResponseEntity<ActivateResponse> activate(@Valid @RequestBody ActivateRequest request) {
		return ResponseEntity.ok(authService.activate(request.activationToken()));
	}

	@PostMapping("/login")
	@Operation(summary = "Verify email and password, then create an email 2FA challenge")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(authService.login(request));
	}

	@PostMapping("/2fa/verify")
	@Operation(summary = "Verify email 2FA code and issue access and refresh tokens")
	public ResponseEntity<VerifyTwoFactorResponse> verifyTwoFactor(@Valid @RequestBody VerifyTwoFactorRequest request) {
		return ResponseEntity.ok(authService.verifyTwoFactor(request));
	}

	@PostMapping("/refresh")
	@Operation(summary = "Rotate refresh token and issue a new JWT access token")
	public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return ResponseEntity.ok(authService.refresh(request.refreshToken()));
	}

	@PostMapping("/logout")
	@Operation(summary = "Revoke a refresh token")
	public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
		authService.logout(request.refreshToken());
		return ResponseEntity.noContent().build();
	}
}
