package com.denden.memberauth.repository;

import com.denden.memberauth.entity.EmailActivationToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailActivationTokenRepository extends JpaRepository<EmailActivationToken, Long> {

	Optional<EmailActivationToken> findByTokenHash(String tokenHash);
}
