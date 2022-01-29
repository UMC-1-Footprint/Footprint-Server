package com.umc.footprint.src.users.jpaRepository;

import com.umc.footprint.src.users.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByOauthId(String oauthId);
}
