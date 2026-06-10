package com.denden.memberauth.config;

import com.denden.memberauth.common.error.ErrorCode;
import com.denden.memberauth.common.error.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.Assert;

@Configuration
public class SecurityConfig {

	private static final ObjectMapper ERROR_RESPONSE_MAPPER = new ObjectMapper();

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
			.csrf(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
				.requestMatchers("/api/auth/register", "/api/auth/activate", "/api/auth/login", "/api/auth/2fa/verify").permitAll()
				.requestMatchers("/api/users/**").authenticated()
				.anyRequest().authenticated()
			)
			.exceptionHandling(exception -> exception.authenticationEntryPoint((request, response, authException) ->
				writeAuthenticationError(response, authException)
			))
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.oauth2ResourceServer(oauth2 -> oauth2
				.authenticationEntryPoint((request, response, authException) -> writeAuthenticationError(response, authException))
				.jwt(Customizer.withDefaults())
			)
			.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	JwtEncoder jwtEncoder(JwtProperties jwtProperties) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey(jwtProperties)));
	}

	@Bean
	JwtDecoder jwtDecoder(JwtProperties jwtProperties) {
		return NimbusJwtDecoder.withSecretKey(jwtSecretKey(jwtProperties))
			.macAlgorithm(MacAlgorithm.HS256)
			.build();
	}

	private SecretKey jwtSecretKey(JwtProperties jwtProperties) {
		Assert.hasText(jwtProperties.secret(), "app.jwt.secret must not be blank");
		Assert.isTrue(jwtProperties.secret().length() >= 32, "app.jwt.secret must be at least 32 characters");
		return new SecretKeySpec(jwtProperties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	private void writeAuthenticationError(HttpServletResponse response, Exception exception) throws IOException {
		ErrorCode errorCode = authenticationErrorCode(exception);
		response.setStatus(errorCode.status().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		ERROR_RESPONSE_MAPPER.writeValue(response.getWriter(), new ErrorResponse(errorCode.name(), errorCode.message()));
	}

	private ErrorCode authenticationErrorCode(Exception exception) {
		if (!(exception instanceof InvalidBearerTokenException invalidBearerTokenException)) {
			return ErrorCode.UNAUTHENTICATED;
		}
		String description = invalidBearerTokenException.getError().getDescription();
		if (containsIgnoreCase(description, "expired")) {
			return ErrorCode.TOKEN_EXPIRED;
		}
		if (containsIgnoreCase(description, "signature")) {
			return ErrorCode.INVALID_TOKEN_SIGNATURE;
		}
		return ErrorCode.INVALID_TOKEN;
	}

	private boolean containsIgnoreCase(String value, String expected) {
		return value != null && value.toLowerCase().contains(expected);
	}
}
