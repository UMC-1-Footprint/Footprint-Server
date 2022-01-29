package com.umc.footprint.src.users.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

// 유저의 refresh 토큰 저장
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "UserRefreshToken")
public class UserRefreshToken {
    @JsonIgnore
    @Id
    @Column(name = "refreshTokenIdx")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long refreshTokenIdx;

    @Column(name = "oauthId", length = 64, unique = true)
    @NotNull
    @Size(max = 64)
    private String oauthId;

    @Column(name = "refreshToken", length = 256)
    @NotNull
    @Size(max = 256)
    private String refreshToken;

    public UserRefreshToken(
            @NotNull @Size(max = 64) String oauthId,
            @NotNull @Size(max = 256) String refreshToken
    ) {
        this.oauthId = oauthId;
        this.refreshToken = refreshToken;
    }
}
