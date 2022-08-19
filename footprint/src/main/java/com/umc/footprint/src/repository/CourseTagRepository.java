package com.umc.footprint.src.repository;

import com.umc.footprint.src.model.CourseTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseTagRepository extends JpaRepository<CourseTag, Integer> {

    List<CourseTag> findAllByCourseIdx(int courseIdx);
}