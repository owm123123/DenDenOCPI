package com.denden.memberauth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.denden.memberauth.entity.EmailActivationToken;
import com.denden.memberauth.entity.LoginTwoFactorCode;
import com.denden.memberauth.entity.User;
import com.denden.memberauth.entity.UserStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class MemberAuthRepositoryTests {

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EmailActivationTokenRepository emailActivationTokenRepository;

	@Autowired
	private LoginTwoFactorCodeRepository loginTwoFactorCodeRepository;

	@Test
	@DisplayName("Should find user by email and check email existence")
	void shouldFindUserByEmailAndCheckEmailExistence() {
		User user = persistUser("member@example.com");

		assertThat(userRepository.findByEmail("member@example.com"))
			.contains(user);
		assertThat(userRepository.existsByEmail("member@example.com"))
			.isTrue();
		assertThat(userRepository.existsByEmail("missing@example.com"))
			.isFalse();
	}

	@Test
	@DisplayName("Should reject duplicate email")
	void shouldRejectDuplicateEmail() {
		persistUser("duplicate@example.com");
		User duplicate = new User(
			"duplicate@example.com",
			"{bcrypt}other-password-hash",
			UserStatus.PENDING_ACTIVATION,
			now(),
			now()
		);

		assertThatThrownBy(() -> entityManager.persistAndFlush(duplicate))
			.isInstanceOf(RuntimeException.class);
	}

	@Test
	@DisplayName("Should find email activation token by token hash")
	void shouldFindEmailActivationTokenByTokenHash() {
		User user = persistUser("activation@example.com");
		EmailActivationToken token = new EmailActivationToken(
			user,
			"activation-token-hash",
			now().plus(1, ChronoUnit.DAYS),
			now()
		);
		entityManager.persistAndFlush(token);

		assertThat(emailActivationTokenRepository.findByTokenHash("activation-token-hash"))
			.contains(token);
		assertThat(emailActivationTokenRepository.findByTokenHash("missing-token-hash"))
			.isEmpty();
	}

	@Test
	@DisplayName("Should find two factor code by challenge id and list latest challenges")
	void shouldFindTwoFactorCodeByChallengeIdAndListLatestChallenges() {
		User user = persistUser("two-factor@example.com");
		LoginTwoFactorCode olderCode = new LoginTwoFactorCode(
			user,
			"11111111-1111-1111-1111-111111111111",
			"older-code-hash",
			now().plus(10, ChronoUnit.MINUTES),
			now().minus(1, ChronoUnit.MINUTES)
		);
		LoginTwoFactorCode newerCode = new LoginTwoFactorCode(
			user,
			"22222222-2222-2222-2222-222222222222",
			"newer-code-hash",
			now().plus(10, ChronoUnit.MINUTES),
			now()
		);
		entityManager.persist(olderCode);
		entityManager.persistAndFlush(newerCode);

		assertThat(loginTwoFactorCodeRepository.findByChallengeId("22222222-2222-2222-2222-222222222222"))
			.contains(newerCode);
		assertThat(loginTwoFactorCodeRepository.findByChallengeId("33333333-3333-3333-3333-333333333333"))
			.isEmpty();
		assertThat(loginTwoFactorCodeRepository.findByUserOrderByCreatedAtDesc(user))
			.containsExactly(newerCode, olderCode);
	}

	private User persistUser(String email) {
		User user = new User(
			email,
			"{bcrypt}password-hash",
			UserStatus.PENDING_ACTIVATION,
			now(),
			now()
		);
		return entityManager.persistAndFlush(user);
	}

	private Instant now() {
		return Instant.now().truncatedTo(ChronoUnit.MILLIS);
	}
}
