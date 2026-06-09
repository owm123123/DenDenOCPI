package com.denden.memberauth.controller.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.denden.memberauth.email.InMemoryActivationEmailSender;
import com.denden.memberauth.entity.EmailActivationToken;
import com.denden.memberauth.entity.User;
import com.denden.memberauth.entity.UserStatus;
import com.denden.memberauth.repository.EmailActivationTokenRepository;
import com.denden.memberauth.repository.UserRepository;
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
}
