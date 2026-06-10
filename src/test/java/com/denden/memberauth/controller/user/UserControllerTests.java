package com.denden.memberauth.controller.user;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.denden.memberauth.common.error.ApiException;
import com.denden.memberauth.common.error.ErrorCode;
import com.denden.memberauth.config.JwtProperties;
import com.denden.memberauth.config.SecurityConfig;
import com.denden.memberauth.dto.user.LastLoginResponse;
import com.denden.memberauth.service.user.UserService;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@EnableConfigurationProperties(JwtProperties.class)
@TestPropertySource(properties = {
	"app.jwt.issuer=http://localhost:8080",
	"app.jwt.secret=test-only-change-me-32-byte-secret-key",
	"app.jwt.access-token-expires-in=PT1H"
})
class UserControllerTests {

	private static final String MEMBER_PUBLIC_ID = "11111111-1111-1111-1111-111111111111";

	private static final String MISSING_PUBLIC_ID = "22222222-2222-2222-2222-222222222222";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtEncoder jwtEncoder;

	@MockitoBean
	private UserService userService;

	@Test
	@DisplayName("Should return current user's last login time")
	void shouldReturnCurrentUsersLastLoginTime() throws Exception {
		when(userService.getMyLastLogin(MEMBER_PUBLIC_ID))
			.thenReturn(new LastLoginResponse("member@example.com", Instant.parse("2026-06-09T08:15:00Z")));

		mockMvc.perform(get("/api/users/last-login")
				.with(jwt().jwt(jwt -> jwt.subject(MEMBER_PUBLIC_ID))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("member@example.com"))
			.andExpect(jsonPath("$.lastLoginAt").value("2026-06-09T08:15:00Z"));
	}

	@Test
	@DisplayName("Should map missing current user to 404")
	void shouldMapMissingCurrentUserToNotFound() throws Exception {
		when(userService.getMyLastLogin(MISSING_PUBLIC_ID))
			.thenThrow(new ApiException(ErrorCode.USER_NOT_FOUND));

		mockMvc.perform(get("/api/users/last-login")
				.with(jwt().jwt(jwt -> jwt.subject(MISSING_PUBLIC_ID))))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
	}

	@Test
	@DisplayName("Should reject last login lookup without JWT")
	void shouldRejectLastLoginLookupWithoutJwt() throws Exception {
		mockMvc.perform(get("/api/users/last-login"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	@DisplayName("Should reject malformed JWT")
	void shouldRejectMalformedJwt() throws Exception {
		mockMvc.perform(get("/api/users/last-login")
				.header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
	}

	@Test
	@DisplayName("Should reject expired JWT")
	void shouldRejectExpiredJwt() throws Exception {
		String expiredToken = accessToken(Instant.parse("2026-06-09T08:15:00Z"));

		mockMvc.perform(get("/api/users/last-login")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
	}

	@Test
	@DisplayName("Should reject JWT with invalid signature")
	void shouldRejectJwtWithInvalidSignature() throws Exception {
		String token = accessToken(Instant.parse("2026-06-11T08:15:00Z"));
		String tamperedToken = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

		mockMvc.perform(get("/api/users/last-login")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + tamperedToken))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_TOKEN_SIGNATURE"));
	}

	private String accessToken(Instant expiresAt) {
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer("http://localhost:8080")
			.subject(MEMBER_PUBLIC_ID)
			.issuedAt(expiresAt.minusSeconds(3600))
			.expiresAt(expiresAt)
			.build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}
}
