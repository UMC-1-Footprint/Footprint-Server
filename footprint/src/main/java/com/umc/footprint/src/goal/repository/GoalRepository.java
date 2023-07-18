package com.umc.footprint.src.goal.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import com.umc.footprint.src.goal.model.entity.Goal;

public interface GoalRepository extends JpaRepository<Goal, Integer> {

    List<Goal> findAllByUserIdx(@Param("userIdx") Integer userIdx);

    Optional<List<Goal>> getByUserIdx(@Param("userIdx") Integer userIdx);
}
