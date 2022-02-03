package com.umc.footprint.oauth.service;

import com.umc.footprint.api.entity.user.OAuthUser;
import com.umc.footprint.api.repository.user.UserRepository;
import com.umc.footprint.oauth.entity.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<OAuthUser> user = userRepository.findByUserId(username);
        if (user.isEmpty()) {
            throw new UsernameNotFoundException("Can not find username.");
        }
        return UserPrincipal.create(user.get());
    }
}
