package com.denden.memberauth.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.email.sendgrid")
public record SendGridEmailProperties(
	String apiKey,
	String senderEmail,
	String senderName,
	String activationBaseUrl
) {

	public String effectiveSenderName() {
		return (senderName == null || senderName.isBlank()) ? "Member Auth" : senderName;
	}
}
