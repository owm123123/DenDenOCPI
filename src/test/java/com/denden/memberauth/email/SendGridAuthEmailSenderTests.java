package com.denden.memberauth.email;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.denden.memberauth.common.error.ApiException;
import com.denden.memberauth.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SendGridAuthEmailSenderTests {

	@Test
	void shouldSendActivationEmailWithSendGridPayload() {
		RestClient.Builder restClientBuilder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
		SendGridAuthEmailSender sender = new SendGridAuthEmailSender(
			new SendGridEmailProperties(
				"sendgrid-api-key",
				"sender@example.com",
				"Member Auth",
				"http://localhost:3000/activate"
			),
			restClientBuilder
		);

		server.expect(requestTo("https://api.sendgrid.com/v3/mail/send"))
			.andExpect(method(HttpMethod.POST))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer sendgrid-api-key"))
			.andExpect(content().json("""
				{
				  "personalizations": [
				    {
				      "to": [
				        {
				          "email": "member@example.com",
				          "name": null
				        }
				      ]
				    }
				  ],
				  "from": {
				    "email": "sender@example.com",
				    "name": "Member Auth"
				  },
				  "subject": "Activate your member account",
				  "content": [
				    {
				      "type": "text/plain",
				      "value": "Please activate your account: http://localhost:3000/activate?activationToken=activation-token"
				    }
				  ],
				  "tracking_settings": {
				    "click_tracking": {
				      "enable": false,
				      "enable_text": false
				    }
				  }
				}
				"""))
			.andRespond(withStatus(HttpStatus.ACCEPTED));

		assertThatNoException()
			.isThrownBy(() -> sender.sendActivationEmail("member@example.com", "activation-token"));

		server.verify();
	}

	@Test
	void shouldMapSendGridUnauthorizedResponseToEmailDeliveryFailed() {
		RestClient.Builder restClientBuilder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
		SendGridAuthEmailSender sender = new SendGridAuthEmailSender(
			new SendGridEmailProperties(
				"sendgrid-api-key",
				"sender@example.com",
				"Member Auth",
				"http://localhost:3000/activate"
			),
			restClientBuilder
		);

		server.expect(requestTo("https://api.sendgrid.com/v3/mail/send"))
			.andExpect(method(HttpMethod.POST))
			.andRespond(withStatus(HttpStatus.UNAUTHORIZED));

		assertThatThrownBy(() -> sender.sendActivationEmail("member@example.com", "activation-token"))
			.isInstanceOf(ApiException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.EMAIL_DELIVERY_FAILED);

		server.verify();
	}
}
