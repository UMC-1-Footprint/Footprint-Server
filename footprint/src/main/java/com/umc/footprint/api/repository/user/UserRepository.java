package com.umc.footprint.api.repository.user;

import com.umc.footprint.api.entity.user.OAuthUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<OAuthUser, Long> {
    OAuthUser findByUserId(String userId);
}
