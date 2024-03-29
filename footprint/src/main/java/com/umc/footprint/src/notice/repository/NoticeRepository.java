package com.umc.footprint.src.notice.repository;

import com.umc.footprint.src.notice.model.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Integer> {

    List<Notice> findAllByStatus(@Param(value = "status") String status);

    List<Notice> findAllByCreateAtAfter(LocalDateTime date);

    List<Notice> findAllByKeyNoticeAndStatus(@Param(value = "keyNotice") boolean keyNotice,@Param(value = "status") String status);
}
