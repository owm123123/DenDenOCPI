package com.denden.memberauth.repository;

import com.denden.memberauth.entity.LoginTwoFactorCode;
import com.denden.memberauth.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginTwoFactorCodeRepository extends JpaRepository<LoginTwoFactorCode, Long> {

	Optional<LoginTwoFactorCode> findByChallengeId(String challengeId);

	List<LoginTwoFactorCode> findByUserOrderByCreatedAtDesc(User user);
}
