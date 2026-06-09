package com.denden.memberauth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 254)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private UserStatus status;

	@Column(name = "activated_at")
	private Instant activatedAt;

	@Column(name = "last_login_at")
	private Instant lastLoginAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(nullable = false)
	private Long version;

	public User(String email, String passwordHash, UserStatus status, Instant createdAt, Instant updatedAt) {
		this.email = email;
		this.passwordHash = passwordHash;
		this.status = status;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public void activate(Instant activatedAt) {
		this.status = UserStatus.ACTIVE;
		this.activatedAt = activatedAt;
		this.updatedAt = activatedAt;
	}
}
