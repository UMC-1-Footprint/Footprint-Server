package com.umc.footprint.src.model;

import com.umc.footprint.utils.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "Course")
public class Course extends BaseEntity {

    @Id
    @Column(name = "courseIdx")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer courseIdx;

    @Column(name = "courseName")
    private String courseName;

    @Column(name = "courseImg")
    private String courseImg;

    @Column(name = "startCoordinate")
    private Point startCoordinate;

    @Column(name = "coordinate", columnDefinition = "longtext")
    private String coordinate;

    @Column(name = "address", columnDefinition = "text")
    private String address;

    @Column(name = "length")
    private Double length;

    @Column(name = "courseTime")
    private Integer courseTime;

    @Column(name = "walkIdx")
    private Integer walkIdx;

    @Column(name = "userIdx")
    private Integer userIdx;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "markNum")
    private Integer markNum;

    @Column(name = "likeNum")
    private Integer likeNum;

    @Column(name = "status")
    private String status;

    @Builder
    public Course(Integer courseIdx, String courseName, String courseImg, Point startCoordinate, String coordinate, String address, Double length, Integer courseTime, Integer walkIdx, Integer userIdx, String description, Integer markNum, Integer likeNum, String status) {
        this.courseIdx = courseIdx;
        this.courseName = courseName;
        this.courseImg = courseImg;
        this.startCoordinate = startCoordinate;
        this.coordinate = coordinate;
        this.address = address;
        this.length = length;
        this.courseTime = courseTime;
        this.walkIdx = walkIdx;
        this.userIdx = userIdx;
        this.description = description;
        this.markNum = markNum;
        this.likeNum = likeNum;
        this.status = status;
    }
}
