package com.umc.footprint.src.users;

import com.umc.footprint.src.users.jpaRepository.UserRepository;
import com.umc.footprint.src.users.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserOAuthService {
    private final UserRepository userRepository;

    public User getUser(String userId) {
        return userRepository.findByOauthId(userId);
    }
}
