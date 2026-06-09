package com.denden.memberauth.email;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class InMemoryActivationEmailSender implements ActivationEmailSender {

	private final List<ActivationEmail> sentEmails = new ArrayList<>();

	@Override
	public void sendActivationEmail(String email, String activationToken) {
		sentEmails.add(new ActivationEmail(email, activationToken));
	}

	public List<ActivationEmail> getSentEmails() {
		return List.copyOf(sentEmails);
	}

	public void clear() {
		sentEmails.clear();
	}

	public record ActivationEmail(String email, String activationToken) {
	}
}
