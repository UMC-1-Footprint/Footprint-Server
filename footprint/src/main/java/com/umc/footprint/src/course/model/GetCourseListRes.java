package com.umc.footprint.src.course.model;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
@Getter
@NoArgsConstructor
public class GetCourseListRes {
    private int courseIdx;
    private double startLat;
    private double startLong;
    private String courseName;
    private Double courseDist;
    private int courseTime;
    private int courseCount;
    private int courseLike;
    private List<String> courseTags;
    private String courseImg;
    private boolean userCourseMark;

    @Builder
    public GetCourseListRes(int courseIdx, double startLat, double startLong, String courseName, Double courseDist, int courseTime, int courseCount, int courseLike, List<String> courseTags, String courseImg, boolean userCourseMark) {
        this.courseIdx = courseIdx;
        this.startLat = startLat;
        this.startLong = startLong;
        this.courseName = courseName;
        this.courseDist = courseDist;
        this.courseTime = courseTime;
        this.courseCount = courseCount;
        this.courseLike = courseLike;
        this.courseTags = courseTags;
        this.courseImg = courseImg;
        this.userCourseMark = userCourseMark;
    }
}
