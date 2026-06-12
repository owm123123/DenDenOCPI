package com.denden.memberauth.integration.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.denden.memberauth.email.InMemoryAuthEmailSender;
import com.denden.memberauth.entity.EmailActivationToken;
import com.denden.memberauth.entity.User;
import com.denden.memberauth.entity.UserStatus;
import com.denden.memberauth.repository.EmailActivationTokenRepository;
import com.denden.memberauth.repository.LoginTwoFactorCodeRepository;
import com.denden.memberauth.repository.RefreshTokenRepository;
import com.denden.memberauth.repository.UserRepository;
import com.denden.memberauth.service.auth.ActivationTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EmailActivationTokenRepository emailActivationTokenRepository;

	@Autowired
	private LoginTwoFactorCodeRepository loginTwoFactorCodeRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ActivationTokenService activationTokenService;

	@Autowired
	private InMemoryAuthEmailSender authEmailSender;

	private final ObjectMapper objectMapper = new ObjectMapper();


	@BeforeEach
	void setUp() {
		authEmailSender.clear();
		refreshTokenRepository.deleteAllInBatch();
		loginTwoFactorCodeRepository.deleteAllInBatch();
		emailActivationTokenRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();
	}

	@Test
	@DisplayName("Should expose actuator health without authentication")
	void shouldExposeActuatorHealthWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/actuator/health"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	@DisplayName("Should register user and send activation email")
	void shouldRegisterUserAndSendActivationEmail() throws Exception {
		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "email": "Member@Example.com",
					  "password": "P@ssw0rd123"
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.message").value("REGISTRATION_CREATED"))
			.andExpect(jsonPath("$.email").value("member@example.com"));

		User user = userRepository.findByEmail("member@example.com").orElseThrow();
		assertThat(user.getStatus()).isEqualTo(UserStatus.PENDING_ACTIVATION);
		assertThat(passwordEncoder.matches("P@ssw0rd123", user.getPasswordHash())).isTrue();
		assertThat(user.getPasswordHash()).doesNotContain("P@ssw0rd123");

		List<EmailActivationToken> tokens = emailActivationTokenRepository.findAll();
		assertThat(tokens).hasSize(1);
		assertThat(tokens.getFirst().getUser().getId()).isEqualTo(user.getId());
		assertThat(tokens.getFirst().getTokenHash()).isNotBlank();

		assertThat(authEmailSender.getActivationEmails())
			.hasSize(1)
			.first()
			.satisfies(email -> {
				assertThat(email.email()).isEqualTo("member@example.com");
				assertThat(email.activationToken()).isNotBlank();
				assertThat(email.activationToken()).isNotEqualTo(tokens.getFirst().getTokenHash());
			});
	}

	@Test
	@DisplayName("Should reject duplicate email registration")
	void shouldRejectDuplicateEmailRegistration() throws Exception {
		register("duplicate@example.com");

		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "email": "DUPLICATE@example.com",
					  "password": "P@ssw0rd123"
					}
					"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));

		assertThat(userRepository.findAll()).hasSize(1);
	}

	@Test
	@DisplayName("Should activate user with valid token")
	void shouldActivateUserWithValidToken() throws Exception {
		register("activate@example.com");
		String activationToken = authEmailSender.getActivationEmails().getFirst().activationToken();

		mockMvc.perform(post("/api/auth/activate")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "activationToken": "%s"
					}
					""".formatted(activationToken)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("ACCOUNT_ACTIVATED"));

		User user = userRepository.findByEmail("activate@example.com").orElseThrow();
		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(user.getActivatedAt()).isNotNull();

		EmailActivationToken token = emailActivationTokenRepository.findAll().getFirst();
		assertThat(token.getUsedAt()).isNotNull();
	}

	@Test
	@DisplayName("Should reject expired activation token")
	void shouldRejectExpiredActivationToken() throws Exception {
		User user = savePendingUser("expired@example.com");
		emailActivationTokenRepository.save(new EmailActivationToken(
			user,
			activationTokenService.hash("expired-token"),
			Instant.parse("2026-06-08T00:00:00Z"),
			Instant.parse("2026-06-07T00:00:00Z")
		));

		mockMvc.perform(post("/api/auth/activate")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "activationToken": "expired-token"
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_ACTIVATION_TOKEN"));

		User unchangedUser = userRepository.findByEmail("expired@example.com").orElseThrow();
		assertThat(unchangedUser.getStatus()).isEqualTo(UserStatus.PENDING_ACTIVATION);
	}

	@Test
	@DisplayName("Should complete login, refresh token rotation, and logout flow")
	void shouldCompleteLoginRefreshTokenRotationAndLogoutFlow() throws Exception {
		register("flow@example.com");
		String activationToken = authEmailSender.getActivationEmails().getFirst().activationToken();
		mockMvc.perform(post("/api/auth/activate")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "activationToken": "%s"
					}
					""".formatted(activationToken)))
			.andExpect(status().isOk());

		String loginResponse = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "email": "flow@example.com",
					  "password": "P@ssw0rd123"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("TWO_FACTOR_REQUIRED"))
			.andReturn()
			.getResponse()
			.getContentAsString();
		String challengeId = objectMapper.readTree(loginResponse).get("challengeId").asText();
		String twoFactorCode = authEmailSender.getTwoFactorEmails().getFirst().code();

		String verifyResponse = mockMvc.perform(post("/api/auth/2fa/verify")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "challengeId": "%s",
					  "code": "%s"
					}
					""".formatted(challengeId, twoFactorCode)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.accessToken").isNotEmpty())
			.andExpect(jsonPath("$.refreshToken").isNotEmpty())
			.andReturn()
			.getResponse()
			.getContentAsString();
		String firstRefreshToken = objectMapper.readTree(verifyResponse).get("refreshToken").asText();
		assertThat(refreshTokenRepository.findAll()).hasSize(1);

		String refreshResponse = mockMvc.perform(post("/api/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "refreshToken": "%s"
					}
					""".formatted(firstRefreshToken)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").isNotEmpty())
			.andExpect(jsonPath("$.refreshToken").isNotEmpty())
			.andReturn()
			.getResponse()
			.getContentAsString();
		String secondRefreshToken = objectMapper.readTree(refreshResponse).get("refreshToken").asText();
		assertThat(secondRefreshToken).isNotEqualTo(firstRefreshToken);
		assertThat(refreshTokenRepository.findAll()).hasSize(2);

		mockMvc.perform(post("/api/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "refreshToken": "%s"
					}
					""".formatted(firstRefreshToken)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

		mockMvc.perform(post("/api/auth/logout")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "refreshToken": "%s"
					}
					""".formatted(secondRefreshToken)))
			.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "refreshToken": "%s"
					}
					""".formatted(secondRefreshToken)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
	}

	private void register(String email) throws Exception {
		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "email": "%s",
					  "password": "P@ssw0rd123"
					}
					""".formatted(email)))
			.andExpect(status().isCreated());
	}

	private User savePendingUser(String email) {
		Instant now = Instant.parse("2026-06-09T00:00:00Z");
		return userRepository.save(new User(
			email,
			passwordEncoder.encode("P@ssw0rd123"),
			UserStatus.PENDING_ACTIVATION,
			now,
			now
		));
	}
}
