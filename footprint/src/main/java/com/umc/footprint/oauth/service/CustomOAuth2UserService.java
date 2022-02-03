package com.umc.footprint.oauth.service;

import com.umc.footprint.api.entity.user.OAuthUser;
import com.umc.footprint.api.repository.user.UserRepository;
import com.umc.footprint.oauth.entity.ProviderType;
import com.umc.footprint.oauth.entity.RoleType;
import com.umc.footprint.oauth.entity.UserPrincipal;
import com.umc.footprint.oauth.exception.OAuthProviderMissMatchException;
import com.umc.footprint.oauth.info.OAuth2UserInfo;
import com.umc.footprint.oauth.info.OAuth2UserInfoFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User user = super.loadUser(userRequest);

        try {
            return this.process(userRequest, user);
        } catch (AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
        }
    }

    private OAuth2User process(OAuth2UserRequest userRequest, OAuth2User user) {
        ProviderType providerType = ProviderType.valueOf(userRequest.getClientRegistration().getRegistrationId().toUpperCase());

        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(providerType, user.getAttributes());
        System.out.println("everything is ok");
        // 디비에서 유저 추출
        Optional<OAuthUser> savedUser = userRepository.findByUserId(userInfo.getId());
        System.out.println("everything is ok");


        //
        if (savedUser.isPresent()) {
            if (providerType != savedUser.get().getProviderType()) {
                throw new OAuthProviderMissMatchException(
                        "Looks like you're signed up with " + providerType +
                        " account. Please use your " + savedUser.get().getProviderType() + " account to login."
                );
            }
            System.out.println("everything is ok1");
            updateUser(savedUser.get(), userInfo);
            System.out.println("everything is ok2");
        } else {
            savedUser = Optional.ofNullable(createUser(userInfo, providerType));
        }

        return UserPrincipal.create(savedUser.get(), user.getAttributes());
    }

    private OAuthUser createUser(OAuth2UserInfo userInfo, ProviderType providerType) {
        LocalDateTime now = LocalDateTime.now();
        OAuthUser user = new OAuthUser(
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

    private OAuthUser updateUser(OAuthUser OAuthUser, OAuth2UserInfo userInfo) {
        if (userInfo.getName() != null && !OAuthUser.getUsername().equals(userInfo.getName())) {
            OAuthUser.setUsername(userInfo.getName());
        }

        return OAuthUser;
    }
}
