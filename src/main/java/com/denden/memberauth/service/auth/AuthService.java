package com.denden.memberauth.service.auth;

import com.denden.memberauth.common.error.ApiException;
import com.denden.memberauth.common.error.ErrorCode;
import com.denden.memberauth.dto.auth.ActivateResponse;
import com.denden.memberauth.dto.auth.LoginRequest;
import com.denden.memberauth.dto.auth.LoginResponse;
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

	private final AuthEmailSender authEmailSender;

	private final Clock clock;

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
		return new VerifyTwoFactorResponse(accessToken.tokenType(), accessToken.accessToken(), accessToken.expiresIn());
	}
}
