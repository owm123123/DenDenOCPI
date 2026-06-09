package com.denden.memberauth.email;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "in-memory", matchIfMissing = true)
public class InMemoryAuthEmailSender implements AuthEmailSender {

	private final List<ActivationEmail> activationEmails = new ArrayList<>();

	private final List<TwoFactorEmail> twoFactorEmails = new ArrayList<>();

	@Override
	public void sendActivationEmail(String email, String activationToken) {
		activationEmails.add(new ActivationEmail(email, activationToken));
	}

	@Override
	public void sendTwoFactorCode(String email, String code) {
		twoFactorEmails.add(new TwoFactorEmail(email, code));
	}

	public List<ActivationEmail> getActivationEmails() {
		return List.copyOf(activationEmails);
	}

	public List<TwoFactorEmail> getTwoFactorEmails() {
		return List.copyOf(twoFactorEmails);
	}

	public void clear() {
		activationEmails.clear();
		twoFactorEmails.clear();
	}

	public record ActivationEmail(String email, String activationToken) {
	}

	public record TwoFactorEmail(String email, String code) {
	}
}
