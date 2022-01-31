package com.umc.footprint.oauth.service;

import com.umc.footprint.oauth.entity.UserPrincipal;
import com.umc.footprint.src.users.jpaRepository.UserRepository;
import com.umc.footprint.src.users.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// 직접 정의한 User 클래스를 spring security 의 내장 User 클래스와 연결하기 위한 Service
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByOauthId(username);
        if (user == null) {
            throw new UsernameNotFoundException("사용자 이름을 찾을 수 없습니다.");
        }

        return UserPrincipal.create(user);
    }
}
