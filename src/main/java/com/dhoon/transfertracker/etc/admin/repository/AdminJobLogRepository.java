package com.dhoon.transfertracker.etc.admin.repository;

import com.dhoon.transfertracker.etc.admin.domain.AdminJobLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminJobLogRepository extends JpaRepository<AdminJobLog, Long> {

    List<AdminJobLog> findTop30ByOrderByCreatedAtDescIdDesc();
}
