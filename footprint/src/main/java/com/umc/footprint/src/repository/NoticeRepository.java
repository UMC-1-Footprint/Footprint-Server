package com.umc.footprint.src.repository;

import com.umc.footprint.src.model.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Integer> {

    List<Notice> findAllByStatus(@Param(value = "status") String status);

    List<Notice> findAllByCreateAtAfter(LocalDateTime date);

    List<Notice> findAllByKeyAndStatus(@Param(value = "key") boolean key,@Param(value = "status") String status);
}
