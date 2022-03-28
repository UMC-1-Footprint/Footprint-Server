package com.umc.footprint.src.repository;

import com.umc.footprint.src.model.Badge;
import com.umc.footprint.src.model.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BadgeRepository extends JpaRepository<Badge, Integer> {
    public List<Badge> findDistinctByBadgeIdx(@Param(value="badgeIdx") Integer badgeIdx); // 뱃지 조회 test
}