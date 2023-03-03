package com.umc.footprint.src.goal.model.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class GoalTest {
    @Test
    void createTest() {
        Goal goal = new Goal(1,1,40,5);
        assertThat(goal.getWalkGoalTime()).isEqualTo(40);
    }

}