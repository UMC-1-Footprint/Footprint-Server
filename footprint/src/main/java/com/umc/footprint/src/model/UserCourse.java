package com.umc.footprint.src.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "UserCourse")
public class UserCourse {
    @Id
    @Column(name = "userCourseIdx")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userCourseIdx;

    @Column(name = "userIdx")
    private Integer userIdx;

    @Column(name = "courseIdx")
    private Integer courseIdx;

    @Column(name = "walkIdx")
    private Integer walkIdx;

    @Column(name = "courseCount")
    private Integer courseCount;
}
