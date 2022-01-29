package com.umc.footprint.src.users.model;

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

// oauth 관련 유저 설정
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "User")
public class User {
    @JsonIgnore
    @Id
    @Column(name = "userIdx")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userIdx;

    @Column(name = "oauthId", length = 64, unique = true)
    @NotNull
    @Size(max = 64)
    private String oauthId;

    @Column(name = "nickname", length = 100)
    @NotNull
    @Size(max = 100)
    private String nickname;

    @JsonIgnore
    @Column(name = "password", length = 128)
    @NotNull
    @Size(max = 128)
    private String password;

    @Column(name = "email", length = 512, unique = true)
    @NotNull
    @Size(max = 512)
    private String email;

    @Column(name = "emailVerifiedYn", length = 1)
    @NotNull
    @Size(min = 1, max = 1)
    private String emailVerifiedYn;

    @Column(name = "providerType", length = 20)
    @Enumerated(EnumType.STRING)
    @NotNull
    private ProviderType providerType;

    @Column(name = "roleType", length = 20)
    @Enumerated(EnumType.STRING)
    @NotNull
    private RoleType roleType;

    @Column(name = "createAt")
    @NotNull
    private LocalDateTime createdAt;

    @Column(name = "updateAt")
    @NotNull
    private LocalDateTime updateAt;

    public User(
                     @NotNull @Size(max = 64) String oauthId,
                     @NotNull @Size(max = 100) String nickname,
                     @NotNull @Size(max = 512) String email,
                     @NotNull @Size(max = 1) String emailVerifiedYn,
                     @NotNull ProviderType providerType,
                     @NotNull RoleType roleType,
                     @NotNull LocalDateTime createdAt,
                     @NotNull LocalDateTime updateAt) {
        this.oauthId = oauthId;
        this.nickname = nickname;
        this.password = "NO_PASS";
        this.email = email != null ? email : "NO_EMAIL";
        this.emailVerifiedYn = emailVerifiedYn;
        this.providerType = providerType;
        this.roleType = roleType;
        this.createdAt = createdAt;
        this.updateAt = updateAt;
    }
}
