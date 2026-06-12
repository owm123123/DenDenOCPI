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

/**
 * 負責簽發本系統的 JWT access token。
 *
 * <p>access token 是短效且 stateless 的登入憑證；server 不保存每一張 access token，
 * 因此撤銷與續期主要由 refresh token 流程負責。</p>
 */
@Service
@RequiredArgsConstructor
public class JwtTokenService {

	private final JwtEncoder jwtEncoder;

	private final JwtProperties jwtProperties;

	private final Clock clock;

	/**
	 * 依使用者資料簽發 access token。
	 *
	 * <p>JWT subject 使用 user publicId，並放入 email 與 type=access claim。
	 * issuedAt 與 expiresAt 均以 UTC Instant 表示，client 可依所在地時區顯示。</p>
	 */
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

	/**
	 * access token 的 API 回傳資訊。
	 *
	 * @param tokenType token 類型，目前固定為 Bearer
	 * @param accessToken JWT access token 本體
	 * @param expiresIn access token 有效秒數
	 */
	public record AccessToken(String tokenType, String accessToken, long expiresIn) {
	}
}
