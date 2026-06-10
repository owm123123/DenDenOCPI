package com.denden.memberauth.email;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.denden.memberauth.common.error.ApiException;
import com.denden.memberauth.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MailjetAuthEmailSenderTests {

	@Test
	void shouldMapMailjetUnauthorizedResponseToEmailDeliveryFailed() {
		RestClient.Builder restClientBuilder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
		MailjetAuthEmailSender sender = new MailjetAuthEmailSender(
			new MailjetEmailProperties(
				"api-key",
				"api-secret",
				"sender@example.com",
				"Member Auth",
				"http://localhost:3000/activate"
			),
			restClientBuilder
		);

		server.expect(requestTo("https://api.mailjet.com/v3.1/send"))
			.andExpect(method(HttpMethod.POST))
			.andRespond(withStatus(HttpStatus.UNAUTHORIZED)
				.contentType(MediaType.APPLICATION_JSON)
				.body("""
					{
					  "ErrorIdentifier": "error-id",
					  "ErrorCode": "mj-0001",
					  "StatusCode": 401,
					  "ErrorMessage": "Your account has been temporarily blocked."
					}
					"""));

		assertThatThrownBy(() -> sender.sendActivationEmail("member@example.com", "activation-token"))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.EMAIL_DELIVERY_FAILED);

		server.verify();
	}
}
