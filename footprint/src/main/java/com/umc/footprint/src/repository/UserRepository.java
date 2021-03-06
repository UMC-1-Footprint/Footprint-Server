package com.umc.footprint.src.repository;

import com.umc.footprint.src.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    boolean existsByEmail(@Param(value = "email") String email);

    User findByEmail(@Param(value = "email") String email);

    User findByUserId(@Param(value = "userId") String userId);

    Optional<User> findByUserIdx(@Param(value = "userIdx") int userIdx);

    Optional<User> getByUserId(@Param(value="userId") String userId);
}
