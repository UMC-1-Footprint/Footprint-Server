package com.umc.footprint.src.users.repository;

import com.umc.footprint.src.users.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    boolean existsByEmail(@Param(value = "email") String email);

    User findByEmail(@Param(value = "email") String email);

    User findByUserId(@Param(value = "userId") String userId);

    @Query(value = "SELECT userIdx FROM User WHERE userId = (:userId)", nativeQuery = true)
    Optional<Integer> getUserIdxByUserId(@Param(value = "userId") String userId);

    Optional<User> findByUserIdx(@Param(value = "userIdx") int userIdx);

    Optional<User> getByUserId(@Param(value="userId") String userId);
}
