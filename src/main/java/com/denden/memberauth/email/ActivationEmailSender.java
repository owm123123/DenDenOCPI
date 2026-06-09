package com.denden.memberauth.email;

public interface ActivationEmailSender {

	void sendActivationEmail(String email, String activationToken);
}
