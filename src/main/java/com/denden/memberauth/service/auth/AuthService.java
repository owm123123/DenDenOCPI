package com.denden.memberauth.service.auth;

import com.denden.memberauth.common.error.ApiException;
import com.denden.memberauth.common.error.ErrorCode;
import com.denden.memberauth.dto.auth.ActivateResponse;
import com.denden.memberauth.dto.auth.LoginRequest;
import com.denden.memberauth.dto.auth.LoginResponse;
import com.denden.memberauth.dto.auth.TokenResponse;
import com.denden.memberauth.dto.auth.RegisterRequest;
import com.denden.memberauth.dto.auth.RegisterResponse;
import com.denden.memberauth.dto.auth.VerifyTwoFactorRequest;
import com.denden.memberauth.dto.auth.VerifyTwoFactorResponse;
import com.denden.memberauth.email.AuthEmailSender;
import com.denden.memberauth.entity.EmailActivationToken;
import com.denden.memberauth.entity.LoginTwoFactorCode;
import com.denden.memberauth.entity.User;
import com.denden.memberauth.entity.UserStatus;
import com.denden.memberauth.repository.EmailActivationTokenRepository;
import com.denden.memberauth.repository.LoginTwoFactorCodeRepository;
import com.denden.memberauth.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 負責會員認證主流程的 application service。
 *
 * <p>這裡串接註冊、Email 開通、登入、Email 二階段驗證、refresh token 更新與登出。
 * Controller 只處理 HTTP request / response，實際商業規則與交易邊界集中在此類別。</p>
 */
@Service
@RequiredArgsConstructor
public class AuthService {

	private static final String REGISTRATION_CREATED = "REGISTRATION_CREATED";

	private static final String ACCOUNT_ACTIVATED = "ACCOUNT_ACTIVATED";

	private static final String TWO_FACTOR_REQUIRED = "TWO_FACTOR_REQUIRED";

	private final UserRepository userRepository;

	private final EmailActivationTokenRepository emailActivationTokenRepository;

	private final LoginTwoFactorCodeRepository loginTwoFactorCodeRepository;

	private final PasswordEncoder passwordEncoder;

	private final ActivationTokenService activationTokenService;

	private final TwoFactorCodeService twoFactorCodeService;

	private final JwtTokenService jwtTokenService;

	private final RefreshTokenService refreshTokenService;

	private final AuthEmailSender authEmailSender;

	private final Clock clock;

	/**
	 * 建立待開通會員，產生 Email activation token，並寄出開通信。
	 *
	 * <p>Email 會先正規化為小寫並去除頭尾空白；密碼只保存 encoder 產生的雜湊值。</p>
	 */
	@Transactional
	public RegisterResponse register(RegisterRequest request) {
		String normalizedEmail = request.email().trim().toLowerCase();
		if (userRepository.existsByEmail(normalizedEmail)) {
			throw new ApiException(ErrorCode.EMAIL_ALREADY_REGISTERED);
		}

		Instant now = clock.instant();
		User user = new User(
			normalizedEmail,
			passwordEncoder.encode(request.password()),
			UserStatus.PENDING_ACTIVATION,
			now,
			now
		);
		User savedUser = userRepository.save(user);

		ActivationTokenService.ActivationToken activationToken = activationTokenService.generate();
		EmailActivationToken token = new EmailActivationToken(
			savedUser,
			activationToken.tokenHash(),
			now.plus(activationToken.expiresIn()),
			now
		);
		emailActivationTokenRepository.save(token);
		authEmailSender.sendActivationEmail(normalizedEmail, activationToken.rawToken());

		return new RegisterResponse(REGISTRATION_CREATED, normalizedEmail);
	}

	/**
	 * 使用 Email 開通信中的 activation token 啟用帳號。
	 *
	 * <p>API 收到的是明文 token，但資料庫只保存 token hash；若 token 已使用或過期，會回傳穩定錯誤碼。</p>
	 */
	@Transactional
	public ActivateResponse activate(String rawToken) {
		Instant now = clock.instant();
		EmailActivationToken token = emailActivationTokenRepository
			.findByTokenHash(activationTokenService.hash(rawToken))
			.orElseThrow(() -> new ApiException(ErrorCode.INVALID_ACTIVATION_TOKEN));

		if (token.isUsed() || token.isExpired(now)) {
			throw new ApiException(ErrorCode.INVALID_ACTIVATION_TOKEN);
		}

		token.getUser().activate(now);
		token.markUsed(now);

		return new ActivateResponse(ACCOUNT_ACTIVATED);
	}

	/**
	 * 驗證 Email 與密碼，成功後建立一次性的二階段驗證 challenge。
	 *
	 * <p>此步驟尚未正式登入，因此不會發 JWT；使用者必須再完成 Email 驗證碼確認。</p>
	 */
	@Transactional
	public LoginResponse login(LoginRequest request) {
		String normalizedEmail = request.email().trim().toLowerCase();
		User user = userRepository
			.findByEmail(normalizedEmail)
			.orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
		}

		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new ApiException(ErrorCode.ACCOUNT_NOT_ACTIVATED);
		}

		Instant now = clock.instant();
		TwoFactorCodeService.TwoFactorCode twoFactorCode = twoFactorCodeService.generate();
		String challengeId = UUID.randomUUID().toString();
		Instant expiresAt = now.plus(twoFactorCode.expiresIn());
		loginTwoFactorCodeRepository.save(new LoginTwoFactorCode(
			user,
			challengeId,
			twoFactorCode.codeHash(),
			expiresAt,
			now
		));
		authEmailSender.sendTwoFactorCode(normalizedEmail, twoFactorCode.rawCode());

		return new LoginResponse(TWO_FACTOR_REQUIRED, challengeId, expiresAt);
	}

	/**
	 * 驗證登入用 Email 二階段驗證碼，成功後發出 access token 與 refresh token。
	 *
	 * <p>驗證成功時會更新使用者最後登入時間；access token 由 JWT 表示，refresh token 會以 hash 形式保存於資料庫。</p>
	 */
	@Transactional
	public VerifyTwoFactorResponse verifyTwoFactor(VerifyTwoFactorRequest request) {
		Instant now = clock.instant();
		LoginTwoFactorCode challenge = loginTwoFactorCodeRepository
			.findByChallengeId(request.challengeId())
			.orElseThrow(() -> new ApiException(ErrorCode.INVALID_TWO_FACTOR_CODE));

		if (challenge.isVerified()) {
			throw new ApiException(ErrorCode.INVALID_TWO_FACTOR_CODE);
		}

		if (challenge.isExpired(now)) {
			throw new ApiException(ErrorCode.TWO_FACTOR_CODE_EXPIRED);
		}

		String codeHash = twoFactorCodeService.hash(request.code());
		if (!challenge.getCodeHash().equals(codeHash)) {
			challenge.increaseFailedAttempts();
			throw new ApiException(ErrorCode.INVALID_TWO_FACTOR_CODE);
		}

		challenge.markVerified(now);
		challenge.getUser().markLoggedIn(now);

		JwtTokenService.AccessToken accessToken = jwtTokenService.issueAccessToken(challenge.getUser());
		RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.issue(challenge.getUser());
		return new VerifyTwoFactorResponse(
			accessToken.tokenType(),
			accessToken.accessToken(),
			accessToken.expiresIn(),
			refreshToken.refreshToken()
		);
	}

	/**
	 * 使用 refresh token 換發新的 access token 與 refresh token。
	 *
	 * <p>refresh token 採 rotation 設計：舊 refresh token 會立即失效，新的 refresh token 取代它。
	 * 已發出的舊 access token 不會主動撤銷，而是依 JWT TTL 自然過期。</p>
	 */
	@Transactional
	public TokenResponse refresh(String rawRefreshToken) {
		RefreshTokenService.RotatedRefreshToken rotatedToken = refreshTokenService.rotate(rawRefreshToken);
		JwtTokenService.AccessToken accessToken = jwtTokenService.issueAccessToken(rotatedToken.user());
		return new TokenResponse(
			accessToken.tokenType(),
			accessToken.accessToken(),
			accessToken.expiresIn(),
			rotatedToken.refreshToken().refreshToken()
		);
	}

	/**
	 * 登出目前 refresh token 所代表的登入工作階段。
	 *
	 * <p>登出會撤銷 refresh token，避免後續再透過它換發 access token。</p>
	 */
	@Transactional
	public void logout(String rawRefreshToken) {
		refreshTokenService.revoke(rawRefreshToken);
	}
}
