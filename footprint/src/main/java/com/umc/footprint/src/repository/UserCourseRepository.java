package com.umc.footprint.src.repository;

import com.umc.footprint.src.model.UserCourse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserCourseRepository extends JpaRepository<UserCourse, Integer> {

    List<UserCourse> findByUserIdx(int userIdx);

    List<UserCourse> findByCourseIdx(int courseIdx);

}
