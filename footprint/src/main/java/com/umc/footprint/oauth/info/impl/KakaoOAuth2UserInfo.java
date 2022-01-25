package com.umc.footprint.oauth.info.impl;

import com.umc.footprint.oauth.info.OAuth2UserInfo;

import java.util.Map;

public class KakaoOAuth2UserInfo extends OAuth2UserInfo {
    public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        return attributes.get("id").toString();
    }

    // kakao에서 받아온 nickname을 name에 저장
    @Override
    public String getName() {
        Map<String, Object> properites = (Map<String, Object>) attributes.get("properties");

        if (properites == null) {
            return null;
        }

        return (String) properites.get("nickname");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("account_email");
    }
}
