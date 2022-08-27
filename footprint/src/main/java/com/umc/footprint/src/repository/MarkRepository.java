package com.umc.footprint.src.repository;

import com.umc.footprint.src.model.Mark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MarkRepository extends JpaRepository<Mark, Integer> {

    List<Mark> findByUserIdx(int userIdx);
}
