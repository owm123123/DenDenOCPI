package com.denden.memberauth.controller.auth;

import com.denden.memberauth.dto.auth.ActivateRequest;
import com.denden.memberauth.dto.auth.ActivateResponse;
import com.denden.memberauth.dto.auth.LoginRequest;
import com.denden.memberauth.dto.auth.LoginResponse;
import com.denden.memberauth.dto.auth.RegisterRequest;
import com.denden.memberauth.dto.auth.RegisterResponse;
import com.denden.memberauth.dto.auth.VerifyTwoFactorRequest;
import com.denden.memberauth.dto.auth.VerifyTwoFactorResponse;
import com.denden.memberauth.service.auth.AuthService;
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
public class AuthController {

	private final AuthService authService;

	@PostMapping("/register")
	public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
		RegisterResponse response = authService.register(request);
		return ResponseEntity
			.status(HttpStatus.CREATED)
			.body(response);
	}

	@PostMapping("/activate")
	public ResponseEntity<ActivateResponse> activate(@Valid @RequestBody ActivateRequest request) {
		return ResponseEntity.ok(authService.activate(request.activationToken()));
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(authService.login(request));
	}

	@PostMapping("/2fa/verify")
	public ResponseEntity<VerifyTwoFactorResponse> verifyTwoFactor(@Valid @RequestBody VerifyTwoFactorRequest request) {
		return ResponseEntity.ok(authService.verifyTwoFactor(request));
	}
}
