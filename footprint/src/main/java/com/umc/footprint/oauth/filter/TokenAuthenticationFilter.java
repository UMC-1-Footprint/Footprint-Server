package com.umc.footprint.oauth.filter;

import com.umc.footprint.oauth.token.AuthToken;
import com.umc.footprint.oauth.token.AuthTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {
    private final AuthTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 요청의 헤더에 있는 access token (string) 반환
        String tokenString = HeaderUtil.getAccessToken(request);
        // 반환된 값으로 토큰 jwt 토큰 생성
        AuthToken token = tokenProvider.convertAuthToken(tokenString);

        if (token.validate()) {
            Authentication authentication = tokenProvider.getAuthentication(token);
            // Security Context Holder에  인증이 완료된 사용자 정보가 저장된 Authentication 객체 저장
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // filter chain에 등록
        filterChain.doFilter(request, response);
    }
}
