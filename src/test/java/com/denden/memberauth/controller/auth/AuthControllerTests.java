package com.denden.memberauth.controller.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.denden.memberauth.email.InMemoryActivationEmailSender;
import com.denden.memberauth.entity.EmailActivationToken;
import com.denden.memberauth.entity.User;
import com.denden.memberauth.entity.UserStatus;
import com.denden.memberauth.repository.EmailActivationTokenRepository;
import com.denden.memberauth.repository.UserRepository;
import com.denden.memberauth.service.auth.ActivationTokenService;
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
class AuthControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EmailActivationTokenRepository emailActivationTokenRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ActivationTokenService activationTokenService;

	@Autowired
	private InMemoryActivationEmailSender activationEmailSender;

	@BeforeEach
	void setUp() {
		activationEmailSender.clear();
		emailActivationTokenRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();
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

		assertThat(activationEmailSender.getSentEmails())
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
	@DisplayName("Should reject invalid register request")
	void shouldRejectInvalidRegisterRequest() throws Exception {
		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "email": "not-an-email",
					  "password": "short"
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	@DisplayName("Should activate user with valid token")
	void shouldActivateUserWithValidToken() throws Exception {
		register("activate@example.com");
		String activationToken = activationEmailSender.getSentEmails().getFirst().activationToken();

		mockMvc.perform(get("/api/auth/activate")
				.param("token", activationToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("ACCOUNT_ACTIVATED"));

		User user = userRepository.findByEmail("activate@example.com").orElseThrow();
		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(user.getActivatedAt()).isNotNull();

		EmailActivationToken token = emailActivationTokenRepository.findAll().getFirst();
		assertThat(token.getUsedAt()).isNotNull();
	}

	@Test
	@DisplayName("Should reject invalid activation token")
	void shouldRejectInvalidActivationToken() throws Exception {
		mockMvc.perform(get("/api/auth/activate")
				.param("token", "invalid-token"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_ACTIVATION_TOKEN"));
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

		mockMvc.perform(get("/api/auth/activate")
				.param("token", "expired-token"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_ACTIVATION_TOKEN"));

		User unchangedUser = userRepository.findByEmail("expired@example.com").orElseThrow();
		assertThat(unchangedUser.getStatus()).isEqualTo(UserStatus.PENDING_ACTIVATION);
	}

	@Test
	@DisplayName("Should reject reused activation token")
	void shouldRejectReusedActivationToken() throws Exception {
		register("reused@example.com");
		String activationToken = activationEmailSender.getSentEmails().getFirst().activationToken();

		mockMvc.perform(get("/api/auth/activate")
				.param("token", activationToken))
			.andExpect(status().isOk());

		mockMvc.perform(get("/api/auth/activate")
				.param("token", activationToken))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_ACTIVATION_TOKEN"));
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
