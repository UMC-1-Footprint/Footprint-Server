package com.umc.footprint.oauth.service;

import com.umc.footprint.oauth.entity.ProviderType;
import com.umc.footprint.oauth.entity.RoleType;
import com.umc.footprint.oauth.entity.UserPrincipal;
import com.umc.footprint.oauth.exception.OAuthProviderMissMatchException;
import com.umc.footprint.oauth.info.OAuth2UserInfo;
import com.umc.footprint.oauth.info.OAuth2UserInfoFactory;
import com.umc.footprint.src.users.UserRepository;
import com.umc.footprint.src.users.model.UserOAuth;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import javax.naming.AuthenticationException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;

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

    private OAuth2User process(OAuth2UserRequest userRequest, OAuth2User user) {
        ProviderType providerType = ProviderType.valueOf(userRequest.getClientRegistration().getRegistrationId().toUpperCase());

        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(providerType, user.getAttributes());
        UserOAuth savedUser = userRepository.findByUserOAuthId(userInfo.getId());

        if (savedUser != null) {
            if (providerType != savedUser.getProviderType()) {
                throw new OAuthProviderMissMatchException(
                        "Looks like you're signed up with " + providerType +
                                " account. Please use your " + savedUser.getProviderType() + " account to login."
                );
            }
            updateUser(savedUser, userInfo);
        } else {
            savedUser = createUser(userInfo, providerType);
        }

        return UserPrincipal.create(savedUser, user.getAttributes());
    }


    private UserOAuth createUser(OAuth2UserInfo userInfo, ProviderType providerType) {
        LocalDateTime now = LocalDateTime.now();
        UserOAuth userOAuth = new UserOAuth(
                userInfo.getId(),
                userInfo.getName(),
                userInfo.getEmail(),
                "Y",
                providerType,
                RoleType.USER,
                now,
                now
        );

        return userRepository.saveAndFlush(userOAuth);
    }

    private UserOAuth updateUser(UserOAuth user, OAuth2UserInfo userInfo) {
        if (userInfo.getName() != null && !user.getUsername().equals(userInfo.getName())) {
            user.setUsername(userInfo.getName());
        }

        return user;
    }
}
