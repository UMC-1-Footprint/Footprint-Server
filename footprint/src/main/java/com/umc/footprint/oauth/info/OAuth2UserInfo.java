package com.umc.footprint.oauth.info;

import java.util.Map;

// 유저 정보의 추상 클래스
public abstract class OAuth2UserInfo {
    protected Map<String, Object> attributes;

    public OAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }


    public abstract String getId();

    public abstract String getName();

    public abstract String getEmail();
}
