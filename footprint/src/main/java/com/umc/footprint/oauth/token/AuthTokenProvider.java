package com.umc.footprint.oauth.token;

import com.umc.footprint.oauth.exception.TokenValidFailedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
public class AuthTokenProvider {
    private final Key key;
    private static final String AUTHORITIES_KEY = "role";

    // jwt secret key 알고리즘을 통해 변환해서 저장
    public AuthTokenProvider(String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public AuthToken createAuthToken(String id, Date expiry) {
        return new AuthToken(id, expiry, this.key);
    }

    public AuthToken createAuthToken(String id, String role, Date expiry) {
        return new AuthToken(id, role, expiry, this.key);
    }

    // access token 또는 refresh token 인증 관련 토큰 (jwt token)으로 변환
    public AuthToken convertAuthToken(String token) {
        return new AuthToken(token, key);
    }

    //
    public Authentication getAuthentication(AuthToken authToken) {
        if (authToken.validate()) {
            // jwt 토큰에 담긴 claims 반환
            Claims claims = authToken.getTokenClaims();
            // 토큰에 저장된 역할 반환
            Collection<? extends GrantedAuthority> authorities = Arrays.stream(new String[]{
                    claims.get(AUTHORITIES_KEY).toString()
            }).map(SimpleGrantedAuthority::new).collect(Collectors.toList());

            log.debug("claims sub");

            User principal = new User(claims.getSubject(), "", authorities);

            // 사용자가 입력한 데이터가 저장된 인증 Authentication 객체 생성후 반환
            return new UsernamePasswordAuthenticationToken(principal, authToken, authorities);
        } else {
            throw new TokenValidFailedException();
        }
    }
}
