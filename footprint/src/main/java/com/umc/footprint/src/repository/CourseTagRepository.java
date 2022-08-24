package com.umc.footprint.src.repository;

import com.umc.footprint.src.model.Course;
import com.umc.footprint.src.model.CourseTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseTagRepository extends JpaRepository<CourseTag, Integer> {
    CourseTag findByCourseAndStatus(Course course, String status);
}
