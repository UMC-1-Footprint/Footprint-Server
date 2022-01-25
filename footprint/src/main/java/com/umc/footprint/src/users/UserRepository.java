package com.umc.footprint.src.users;

import com.umc.footprint.src.users.model.UserOAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserOAuth, Long> {
    UserOAuth findByUserOAuthId(String userId);
}
