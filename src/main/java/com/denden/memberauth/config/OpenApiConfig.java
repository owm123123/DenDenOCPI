package com.denden.memberauth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	private static final String BEARER_AUTH_SCHEME = "bearerAuth";

	@Bean
	OpenAPI memberAuthOpenApi() {
		return new OpenAPI()
			.info(new Info()
				.title("DenDen Member Auth API")
				.version("0.0.1")
				.description("Member registration, email activation, login, email 2FA, and last-login APIs."))
			.components(new Components()
				.addSecuritySchemes(BEARER_AUTH_SCHEME, new SecurityScheme()
					.type(SecurityScheme.Type.HTTP)
					.scheme("bearer")
					.bearerFormat("JWT")));
	}
}
