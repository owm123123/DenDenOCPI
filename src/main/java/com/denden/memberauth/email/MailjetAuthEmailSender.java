package com.denden.memberauth.email;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "mailjet")
public class MailjetAuthEmailSender implements AuthEmailSender {

	private static final String MAILJET_SEND_API = "https://api.mailjet.com/v3.1/send";

	private final MailjetEmailProperties properties;

	private final RestClient restClient;

	public MailjetAuthEmailSender(MailjetEmailProperties properties, RestClient.Builder restClientBuilder) {
		this.properties = properties;
		this.restClient = restClientBuilder
			.defaultHeaders(headers -> headers.setBasicAuth(
				required(properties.apiKey(), "apiKey"),
				required(properties.apiSecret(), "apiSecret")
			))
			.build();
		required(properties.senderEmail(), "senderEmail");
		required(properties.activationBaseUrl(), "activationBaseUrl");
	}

	@Override
	public void sendActivationEmail(String email, String activationToken) {
		String activationLink = UriComponentsBuilder
			.fromUriString(properties.activationBaseUrl())
			.queryParam("token", activationToken)
			.build()
			.toUriString();

		sendEmail(
			email,
			"Activate your member account",
			"Please activate your account: " + activationLink
		);
	}

	@Override
	public void sendTwoFactorCode(String email, String code) {
		sendEmail(
			email,
			"Your two-factor verification code",
			"Your verification code is " + code + ". It will expire soon."
		);
	}

	private void sendEmail(String email, String subject, String textPart) {
		MailjetSendRequest request = new MailjetSendRequest(List.of(new MailjetMessage(
			new MailjetContact(properties.senderEmail(), properties.effectiveSenderName()),
			List.of(new MailjetContact(email, null)),
			subject,
			textPart
		)));

		restClient
			.post()
			.uri(MAILJET_SEND_API)
			.contentType(MediaType.APPLICATION_JSON)
			.body(request)
			.retrieve()
			.toBodilessEntity();
	}

	private static String required(String value, String propertyName) {
		if (value == null || value.isBlank()) {
			throw new IllegalStateException("Missing Mailjet email property: app.email.mailjet." + propertyName);
		}
		return value;
	}

	private record MailjetSendRequest(List<MailjetMessage> Messages) {
	}

	private record MailjetMessage(MailjetContact From, List<MailjetContact> To, String Subject, String TextPart) {
	}

	private record MailjetContact(String Email, String Name) {
	}
}
