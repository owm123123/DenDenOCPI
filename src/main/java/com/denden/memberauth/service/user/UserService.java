package com.denden.memberauth.service.user;

import com.denden.memberauth.common.error.ApiException;
import com.denden.memberauth.common.error.ErrorCode;
import com.denden.memberauth.dto.user.LastLoginResponse;
import com.denden.memberauth.entity.User;
import com.denden.memberauth.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository userRepository;

	public LastLoginResponse getMyLastLogin(String publicId) {
		User user = userRepository
			.findByPublicId(toPublicId(publicId))
			.orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

		return new LastLoginResponse(user.getEmail(), user.getLastLoginAt());
	}

	private UUID toPublicId(String publicId) {
		try {
			return UUID.fromString(publicId);
		}
		catch (IllegalArgumentException exception) {
			throw new ApiException(ErrorCode.USER_NOT_FOUND);
		}
	}
}
