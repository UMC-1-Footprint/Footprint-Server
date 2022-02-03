package com.umc.footprint.api.entity.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.umc.footprint.oauth.entity.ProviderType;
import com.umc.footprint.oauth.entity.RoleType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OAuthUser {
    private Long userSeq;

    private String userId;

    private String username;

    private String password;

    private String email;

    private String emailVerifiedYn;

    private ProviderType providerType;

    private RoleType roleType;

    private LocalDateTime createdAt;

    private LocalDateTime modifiedAt;

    public OAuthUser(
            Long userSeq,
            String userId,
            String username,
            String email,
            String emailVerifiedYn,
            String providerType,
            String roleType,
            LocalDateTime createdAt,
            LocalDateTime modifiedAt
    ) {
        this.userSeq = userSeq;
        this.userId = userId;
        this.username = username;
        this.password = "NO_PASS";
        this.email = email != null ? email : "NO_EMAIL";
        this.emailVerifiedYn = emailVerifiedYn;
        System.out.println("providerType = " + providerType);
        System.out.println("providerType.getClass().getName() = " + providerType.getClass().getName());
        if (providerType.equals(ProviderType.GOOGLE.toString())) {
            this.providerType = ProviderType.GOOGLE;
        } else if (providerType.equals(ProviderType.KAKAO.toString())) {
            this.providerType = ProviderType.KAKAO;
        } else {
            this.providerType = ProviderType.LOCAL;
        }

        if (roleType.equals(RoleType.USER.toString())) {
            this.roleType = RoleType.USER;
        } else if (roleType.equals(RoleType.ADMIN.toString())) {
            this.roleType = RoleType.ADMIN;
        } else {
            this.roleType = RoleType.GUEST;
        }
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
    }

    public OAuthUser(
            @NotNull @Size(max = 64) String userId,
            @NotNull @Size(max = 100) String username,
            @NotNull @Size(max = 512) String email,
            @NotNull @Size(max = 1) String emailVerifiedYn,
            @NotNull ProviderType providerType,
            @NotNull RoleType roleType,
            @NotNull LocalDateTime createdAt,
            @NotNull LocalDateTime modifiedAt
    ) {
        this.userId = userId;
        this.username = username;
        this.password = "NO_PASS";
        this.email = email != null ? email : "NO_EMAIL";
        this.emailVerifiedYn = emailVerifiedYn;
        this.providerType = providerType;
        this.roleType = roleType;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
    }
}
