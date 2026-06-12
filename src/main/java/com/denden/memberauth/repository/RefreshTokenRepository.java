package com.denden.memberauth.repository;

import com.denden.memberauth.entity.RefreshToken;
import com.denden.memberauth.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	List<RefreshToken> findByUserOrderByCreatedAtDesc(User user);
}
