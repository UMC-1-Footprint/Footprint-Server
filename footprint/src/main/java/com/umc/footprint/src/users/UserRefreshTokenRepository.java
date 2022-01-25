package com.umc.footprint.src.users;

import com.umc.footprint.src.users.model.UserRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRefreshTokenRepository extends JpaRepository<UserRefreshToken, Long> {
    UserRefreshToken findByUserId(String userId);

    UserRefreshToken findByUserIdAAndRefreshToken(String userId, String refreshToken);
}
