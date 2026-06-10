package com.denden.memberauth.email;

import com.denden.memberauth.common.error.ApiException;
import com.denden.memberauth.common.error.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
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
@ConditionalOnProperty(name = "app.email.provider", havingValue = "mailjet")
public class MailjetAuthEmailSender implements AuthEmailSender {

	private static final String MAILJET_SEND_API = "https://api.mailjet.com/v3.1/send";

	private static final ObjectMapper MAILJET_ERROR_MAPPER = new ObjectMapper();

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
		MailjetSendRequest request = new MailjetSendRequest(List.of(new MailjetMessage(
			new MailjetContact(properties.senderEmail(), properties.effectiveSenderName()),
			List.of(new MailjetContact(email, null)),
			subject,
			textPart
		)));

		try {
			restClient
				.post()
				.uri(MAILJET_SEND_API)
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
			MailjetErrorResponse mailjetError = parseMailjetError(responseException.getResponseBodyAsString());
			log.warn(
				"Mailjet rejected email send. statusCode={}, mailjetErrorCode={}, mailjetErrorIdentifier={}, mailjetMessage={}",
				responseException.getStatusCode(),
				mailjetError.errorCode(),
				mailjetError.errorIdentifier(),
				mailjetError.errorMessage()
			);
		}
		else {
			log.warn("Mailjet email send failed: {}", exception.getClass().getSimpleName());
		}
		throw new ApiException(ErrorCode.EMAIL_DELIVERY_FAILED);
	}

	private MailjetErrorResponse parseMailjetError(String responseBody) {
		if (responseBody == null || responseBody.isBlank()) {
			return MailjetErrorResponse.empty();
		}
		try {
			return MAILJET_ERROR_MAPPER.readValue(responseBody, MailjetErrorResponse.class);
		}
		catch (JsonProcessingException exception) {
			return MailjetErrorResponse.unparseable();
		}
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

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record MailjetErrorResponse(
		@JsonProperty("ErrorIdentifier") String errorIdentifier,
		@JsonProperty("ErrorCode") String errorCode,
		@JsonProperty("ErrorMessage") String errorMessage
	) {

		private static MailjetErrorResponse empty() {
			return new MailjetErrorResponse("unknown", "unknown", "empty response body");
		}

		private static MailjetErrorResponse unparseable() {
			return new MailjetErrorResponse("unknown", "unknown", "unparseable response body");
		}
	}
}
