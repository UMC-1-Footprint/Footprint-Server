package com.umc.footprint.src.users.model;

import com.umc.footprint.src.model.User;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

@Getter
@NoArgsConstructor
public class PostLoginReq {

    @ApiModelProperty(example = "사용자 ID")
    private String userId;

    @ApiModelProperty(example = "사용자 이름")
    private String username;

    @ApiModelProperty(example = "사용자 Email")
    private String email;

    @ApiModelProperty(example = "구글 Or 카카오")
    private String providerType;

    @Builder
    public PostLoginReq(String userId, String username, String email, String providerType) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.providerType = providerType;
    }

    public void setEncryptedInfos(String encryptedUsername, String encryptedEmail) {
        this.username = encryptedUsername;
        this.email = encryptedEmail;
    }

    public User toUserEntity() {
        return User.builder()
                .userId(userId)
                .username(username)
                .email(email)
                .providerType(providerType)
                .build();
    }
}
