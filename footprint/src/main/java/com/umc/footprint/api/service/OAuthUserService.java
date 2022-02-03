package com.umc.footprint.api.service;

import com.umc.footprint.api.entity.user.OAuthUser;
import com.umc.footprint.api.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OAuthUserService {
    private final UserRepository userRepository;

    public Optional<OAuthUser> getOAuthUser(String userId) {
        return userRepository.findByUserId(userId);
    }
}
