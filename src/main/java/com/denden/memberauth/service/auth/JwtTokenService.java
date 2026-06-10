package com.denden.memberauth.service.auth;

import com.denden.memberauth.config.JwtProperties;
import com.denden.memberauth.entity.User;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtTokenService {

	private final JwtEncoder jwtEncoder;

	private final JwtProperties jwtProperties;

	private final Clock clock;

	public AccessToken issueAccessToken(User user) {
		Instant issuedAt = clock.instant();
		Instant expiresAt = issuedAt.plus(jwtProperties.accessTokenExpiresIn());
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(jwtProperties.issuer())
			.issuedAt(issuedAt)
			.expiresAt(expiresAt)
			.subject(user.getPublicId().toString())
			.claim("email", user.getEmail())
			.claim("type", "access")
			.build();

		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new AccessToken("Bearer", token, jwtProperties.accessTokenExpiresIn().toSeconds());
	}

	public record AccessToken(String tokenType, String accessToken, long expiresIn) {
	}
}
