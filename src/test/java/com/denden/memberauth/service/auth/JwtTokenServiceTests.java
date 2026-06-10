package com.denden.memberauth.service.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.denden.memberauth.config.JwtProperties;
import com.denden.memberauth.entity.User;
import com.denden.memberauth.entity.UserStatus;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class JwtTokenServiceTests {

	private static final Instant NOW = Instant.parse("2030-06-09T00:00:00Z");

	private static final String SECRET = "test-secret-change-me-32-byte-key";

	@Test
	@DisplayName("Should issue decodable access token with stable claims")
	void shouldIssueDecodableAccessTokenWithStableClaims() {
		SecretKey secretKey = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
		JwtProperties jwtProperties = new JwtProperties("http://localhost:8080", SECRET, Duration.ofHours(1));
		JwtTokenService jwtTokenService = new JwtTokenService(
			new NimbusJwtEncoder(new ImmutableSecret<>(secretKey)),
			jwtProperties,
			Clock.fixed(NOW, ZoneOffset.UTC)
		);
		JwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey)
			.macAlgorithm(MacAlgorithm.HS256)
			.build();
		User user = new User(
			"member@example.com",
			"encoded-password",
			UserStatus.ACTIVE,
			NOW.minus(Duration.ofDays(1)),
			NOW.minus(Duration.ofDays(1))
		);

		JwtTokenService.AccessToken accessToken = jwtTokenService.issueAccessToken(user);
		Jwt jwt = jwtDecoder.decode(accessToken.accessToken());

		assertThat(accessToken.tokenType()).isEqualTo("Bearer");
		assertThat(accessToken.expiresIn()).isEqualTo(3600);
		assertThat(jwt.getIssuer().toString()).isEqualTo("http://localhost:8080");
		assertThat(jwt.getSubject()).isEqualTo(user.getPublicId().toString());
		assertThat(jwt.getClaimAsString("email")).isEqualTo("member@example.com");
		assertThat(jwt.getClaimAsString("type")).isEqualTo("access");
		assertThat(jwt.getIssuedAt()).isEqualTo(NOW);
		assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(1)));
	}
}
