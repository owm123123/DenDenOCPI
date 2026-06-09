package com.denden.memberauth.service.user;

import com.denden.memberauth.common.error.ApiException;
import com.denden.memberauth.common.error.ErrorCode;
import com.denden.memberauth.dto.user.LastLoginResponse;
import com.denden.memberauth.entity.User;
import com.denden.memberauth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository userRepository;

	public LastLoginResponse getMyLastLogin(String email) {
		User user = userRepository
			.findByEmail(email)
			.orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

		return new LastLoginResponse(user.getEmail(), user.getLastLoginAt());
	}
}
