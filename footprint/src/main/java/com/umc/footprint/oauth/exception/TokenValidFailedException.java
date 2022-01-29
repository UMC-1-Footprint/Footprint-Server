package com.umc.footprint.oauth.exception;

// 토큰 validation이 실패했을 때
public class TokenValidFailedException extends RuntimeException {
    public TokenValidFailedException() {
        super("토큰 생성 실패");
    }

    public TokenValidFailedException(String message) {
        super(message);
    }
}
