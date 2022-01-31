package com.umc.footprint.oauth.service;

import com.umc.footprint.oauth.entity.ProviderType;
import com.umc.footprint.oauth.entity.RoleType;
import com.umc.footprint.oauth.entity.UserPrincipal;
import com.umc.footprint.oauth.exception.OAuthProviderMissMatchException;
import com.umc.footprint.oauth.info.OAuth2UserInfo;
import com.umc.footprint.oauth.info.OAuth2UserInfoFactory;
import com.umc.footprint.src.users.jpaRepository.UserRepository;
import com.umc.footprint.src.users.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import javax.naming.AuthenticationException;
import java.time.LocalDateTime;

// 유저의 정보를 가져오는 역할 수행, 가져온 정보는 UserPrincipal 클래스로 변경하여 spring security 에 전달
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;

    // oauth2 공급자로부터 사용자의 정보 추출
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User user = super.loadUser(userRequest);

        try {
            return this.process(userRequest, user);
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new InternalAuthenticationServiceException(exception.getMessage(), exception.getCause());
        }
    }

    //
    private OAuth2User process(OAuth2UserRequest userRequest, OAuth2User user) {
        // 인증을 요청받는 공급자 타입
        ProviderType providerType = ProviderType.valueOf(userRequest.getClientRegistration().getRegistrationId().toUpperCase());

        // 공급자(구글, 카카오)가 제공하는 유저 정보
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(providerType, user.getAttributes());
        // db에 있는 유저, 유저 id로 조회
        User savedUser = userRepository.findByOauthId(userInfo.getId());

        // 이미 존재하는 유저인지 확인
        if (savedUser != null) {
            if (providerType != savedUser.getProviderType()) {
                throw new OAuthProviderMissMatchException(
                        "OAuth2 공급자(구글, 카카오)에서 이메일을 찾을 수 없습니다. " + savedUser.getProviderType() + " 으로 시도해보십시오."
                );
            }
            // 유저 정보 업데이트
            updateUser(savedUser, userInfo);
        } else {
            // 처음 등록하는 유저인 경우 유저 생성
            savedUser = createUser(userInfo, providerType);
        }

        // 유저의 정보를 UserPrincipal 클래스에 담아 반환
        return UserPrincipal.create(savedUser, user.getAttributes());
    }

    // 유저 정보로 유저 db에 저장, 유저 반환
    private User createUser(OAuth2UserInfo userInfo, ProviderType providerType) {
        LocalDateTime now = LocalDateTime.now();
        User user = new User(
                userInfo.getId(),
                userInfo.getName(),
                userInfo.getEmail(),
                "Y",
                providerType,
                RoleType.USER,
                now,
                now
        );

        return userRepository.saveAndFlush(user);
    }

    // 유저 정보 업데이트
    private User updateUser(User user, OAuth2UserInfo userInfo) {
        if (userInfo.getName() != null && !user.getNickname().equals(userInfo.getName())) {
            user.setNickname(userInfo.getName());
        }

        return user;
    }

}
