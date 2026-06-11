package com.denden.memberauth.email;

import com.denden.memberauth.common.error.ApiException;
import com.denden.memberauth.common.error.ErrorCode;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "sendgrid")
public class SendGridAuthEmailSender implements AuthEmailSender {

	private static final String SENDGRID_SEND_API = "https://api.sendgrid.com/v3/mail/send";

	private final SendGridEmailProperties properties;

	private final RestClient restClient;

	public SendGridAuthEmailSender(SendGridEmailProperties properties, RestClient.Builder restClientBuilder) {
		this.properties = properties;
		this.restClient = restClientBuilder
			.defaultHeaders(headers -> headers.setBearerAuth(required(properties.apiKey(), "apiKey")))
			.build();
		required(properties.senderEmail(), "senderEmail");
		required(properties.activationBaseUrl(), "activationBaseUrl");
	}

	@Override
	public void sendActivationEmail(String email, String activationToken) {
		String activationLink = UriComponentsBuilder
			.fromUriString(properties.activationBaseUrl())
			.queryParam("activationToken", activationToken)
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
		SendGridSendRequest request = new SendGridSendRequest(
			List.of(new SendGridPersonalization(List.of(new SendGridEmail(email)))),
			new SendGridEmail(properties.senderEmail(), properties.effectiveSenderName()),
			subject,
			List.of(new SendGridContent("text/plain", textPart)),
			SendGridTrackingSettings.clickTrackingDisabled()
		);

		try {
			restClient
				.post()
				.uri(SENDGRID_SEND_API)
				.contentType(MediaType.APPLICATION_JSON)
				.body(request)
				.retrieve()
				.toBodilessEntity();
		}
		catch (RestClientException exception) {
			handleEmailDeliveryFailure(exception);
		}
	}

	private void handleEmailDeliveryFailure(Exception exception) {
		if (exception instanceof RestClientResponseException responseException) {
			log.warn("SendGrid rejected email send. statusCode={}", responseException.getStatusCode());
		}
		else {
			log.warn("SendGrid email send failed: {}", exception.getClass().getSimpleName());
		}
		throw new ApiException(ErrorCode.EMAIL_DELIVERY_FAILED);
	}

	private static String required(String value, String propertyName) {
		if (value == null || value.isBlank()) {
			throw new IllegalStateException("Missing SendGrid email property: app.email.sendgrid." + propertyName);
		}
		return value;
	}

	private record SendGridSendRequest(
		List<SendGridPersonalization> personalizations,
		SendGridEmail from,
		String subject,
		List<SendGridContent> content,
		@JsonProperty("tracking_settings") SendGridTrackingSettings trackingSettings
	) {
	}

	private record SendGridPersonalization(List<SendGridEmail> to) {
	}

	private record SendGridEmail(String email, String name) {

		private SendGridEmail(String email) {
			this(email, null);
		}
	}

	private record SendGridContent(String type, String value) {
	}

	private record SendGridTrackingSettings(@JsonProperty("click_tracking") SendGridClickTracking clickTracking) {

		private static SendGridTrackingSettings clickTrackingDisabled() {
			return new SendGridTrackingSettings(new SendGridClickTracking(false, false));
		}
	}

	private record SendGridClickTracking(boolean enable, @JsonProperty("enable_text") boolean enableText) {
	}
}
