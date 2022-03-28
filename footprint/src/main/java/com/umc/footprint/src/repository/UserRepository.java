package com.umc.footprint.src.repository;

import com.umc.footprint.src.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Integer> {
    boolean existsByEmail(@Param(value = "email") String email);

    User findByEmail(@Param(value = "email") String email);

    User findByUserId(@Param(value = "userId") String userId);
}
