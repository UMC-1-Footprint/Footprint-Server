package com.umc.footprint.src.users.jpaRepository;

import com.umc.footprint.src.users.model.UserRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRefreshTokenRepository extends JpaRepository<UserRefreshToken, Long> {
    UserRefreshToken findByOauthId(String oauthId);
    UserRefreshToken findByOauthIdAndAndRefreshToken(String oauthId, String refreshToken);
}
