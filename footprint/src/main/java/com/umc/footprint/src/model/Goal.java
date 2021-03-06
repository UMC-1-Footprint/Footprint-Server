package com.umc.footprint.src.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "Goal")
public class Goal {

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

    @Column(name = "createAt")
    private LocalDateTime createAt;

    @Builder
    public Goal(Integer planIdx, Integer userIdx, Integer walkGoalTime, Integer walkTimeSlot, LocalDateTime createAt) {
        this.planIdx = planIdx;
        this.userIdx = userIdx;
        this.walkGoalTime = walkGoalTime;
        this.walkTimeSlot = walkTimeSlot;
        this.createAt = createAt;
    }

    @PrePersist
    public void prePersist() {
        this.createAt = this.createAt == null ? LocalDateTime.now() : this.createAt;
    }
}
