package com.umc.footprint.api.entity.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRefreshToken {
    private Long refreshTokenSeq;

    private String userId;

    private String refreshToken;

    public UserRefreshToken(
            String userId,
            String refreshToken
    ) {
        this.userId = userId;
        this.refreshToken = refreshToken;
    }
}
