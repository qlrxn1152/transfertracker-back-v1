package com.dhoon.transfertracker.internal.notice.repository;

import com.dhoon.transfertracker.internal.notice.domain.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findTop5ByActiveTrueOrderByCreatedAtDescIdDesc();
}
