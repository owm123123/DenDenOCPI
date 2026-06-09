package com.denden.memberauth.service.auth;

import com.denden.memberauth.common.error.ApiException;
import com.denden.memberauth.common.error.ErrorCode;
import com.denden.memberauth.dto.auth.ActivateResponse;
import com.denden.memberauth.dto.auth.RegisterRequest;
import com.denden.memberauth.dto.auth.RegisterResponse;
import com.denden.memberauth.email.ActivationEmailSender;
import com.denden.memberauth.entity.EmailActivationToken;
import com.denden.memberauth.entity.User;
import com.denden.memberauth.entity.UserStatus;
import com.denden.memberauth.repository.EmailActivationTokenRepository;
import com.denden.memberauth.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

	private static final String REGISTRATION_CREATED = "REGISTRATION_CREATED";

	private static final String ACCOUNT_ACTIVATED = "ACCOUNT_ACTIVATED";

	private final UserRepository userRepository;

	private final EmailActivationTokenRepository emailActivationTokenRepository;

	private final PasswordEncoder passwordEncoder;

	private final ActivationTokenService activationTokenService;

	private final ActivationEmailSender activationEmailSender;

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
		activationEmailSender.sendActivationEmail(normalizedEmail, activationToken.rawToken());

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
}
