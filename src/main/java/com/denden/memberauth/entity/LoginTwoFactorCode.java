package com.denden.memberauth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "login_two_factor_codes")
public class LoginTwoFactorCode {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "challenge_id", nullable = false, length = 36)
	private String challengeId;

	@Column(name = "code_hash", nullable = false, length = 128)
	private String codeHash;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "verified_at")
	private Instant verifiedAt;

	@Column(name = "failed_attempts", nullable = false)
	private int failedAttempts;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected LoginTwoFactorCode() {
	}

	public LoginTwoFactorCode(User user, String challengeId, String codeHash, Instant expiresAt, Instant createdAt) {
		this.user = user;
		this.challengeId = challengeId;
		this.codeHash = codeHash;
		this.expiresAt = expiresAt;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public String getChallengeId() {
		return challengeId;
	}

	public String getCodeHash() {
		return codeHash;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getVerifiedAt() {
		return verifiedAt;
	}

	public int getFailedAttempts() {
		return failedAttempts;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
