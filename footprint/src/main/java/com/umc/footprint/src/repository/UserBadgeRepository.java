package com.umc.footprint.src.repository;

import com.umc.footprint.src.model.Badge;
import com.umc.footprint.src.model.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Integer> {
    public List<UserBadge> findDistinctByUserIdx(@Param(value="userIdx") Integer userIdx); // 뱃지 조회 test
}
