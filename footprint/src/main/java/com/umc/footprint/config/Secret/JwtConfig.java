package com.umc.footprint.config.Secret;

import com.umc.footprint.oauth.token.AuthTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {
    // application.yml에 명시된 secret key
    @Value("${jwt.secret}")
    private String secret;

    // jwt 토큰 provider에 secret key 전달
    @Bean
    public AuthTokenProvider jwtProvider() {
        return new AuthTokenProvider(secret);
    }
}
