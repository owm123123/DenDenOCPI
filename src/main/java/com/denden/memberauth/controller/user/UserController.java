package com.denden.memberauth.controller.user;

import com.denden.memberauth.dto.user.LastLoginResponse;
import com.denden.memberauth.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@GetMapping("/last-login")
	public ResponseEntity<LastLoginResponse> getMyLastLogin(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok(userService.getMyLastLogin(jwt.getSubject()));
	}
}
