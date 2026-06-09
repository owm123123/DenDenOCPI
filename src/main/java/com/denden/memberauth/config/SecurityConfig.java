package com.denden.memberauth.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.Assert;

@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
			.csrf(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/api/auth/register", "/api/auth/activate", "/api/auth/login", "/api/auth/2fa/verify").permitAll()
				.requestMatchers("/api/users/**").authenticated()
				.anyRequest().authenticated()
			)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
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
}
