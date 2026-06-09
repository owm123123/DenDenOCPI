package com.denden.memberauth.email;

public interface AuthEmailSender {

	void sendActivationEmail(String email, String activationToken);

	void sendTwoFactorCode(String email, String code);
}
