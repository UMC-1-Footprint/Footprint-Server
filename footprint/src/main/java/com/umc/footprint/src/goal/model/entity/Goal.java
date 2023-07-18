package com.umc.footprint.src.goal.model.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.umc.footprint.config.Constant;
import com.umc.footprint.utils.BaseEntity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "Goal")
public class Goal extends BaseEntity {

    private static final long MINUTE_TO_SECONDS_VALUE = 60L;
    @Id
    @Column(name = "planIdx")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer planIdx;

    @Column(name = "userIdx", nullable = false)
    private Integer userIdx;

    @Column(name = "walkGoalTime", nullable = false)
    private Integer walkGoalTime;

    @Column(name = "walkTimeSlot", nullable = false)
    private Integer walkTimeSlot;

    @Builder
    public Goal(Integer planIdx, Integer userIdx, Integer walkGoalTime, Integer walkTimeSlot) {
        this.planIdx = planIdx;
        this.userIdx = userIdx;
        this.walkGoalTime = walkGoalTime;
        this.walkTimeSlot = walkTimeSlot;
    }

    public Double computeGoalRate(Long walkTime) {
        double goalRate = walkTime.doubleValue() / getWalkGoalTimeAsSeconds() * 100.0;
        return Math.min(goalRate, 100.0);
    }

    private long getWalkGoalTimeAsSeconds() {
        return walkGoalTime * Constant.MINUTES_TO_SECONDS;
    }
}
