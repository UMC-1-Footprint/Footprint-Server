package com.umc.footprint.oauth.service;

import com.umc.footprint.oauth.entity.UserPrincipal;
import com.umc.footprint.src.users.UserRepository;
import com.umc.footprint.src.users.model.UserOAuth;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserOAuth user = userRepository.findByUserOAuthId(username);
        if (user == null) {
            throw new UsernameNotFoundException("Can not find username");
        }

        return UserPrincipal.create(user);
    }
}
