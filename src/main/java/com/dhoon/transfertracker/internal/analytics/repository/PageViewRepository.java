package com.dhoon.transfertracker.internal.analytics.repository;

import com.dhoon.transfertracker.internal.analytics.domain.PageView;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface PageViewRepository extends JpaRepository<PageView, Long> {

    long countByCreatedAtGreaterThanEqual(LocalDateTime createdAt);

    @Query("select count(distinct pv.visitorId) from PageView pv where pv.createdAt >= :createdAt")
    long countDistinctVisitorsByCreatedAtGreaterThanEqual(LocalDateTime createdAt);

    @Query("select pv.path, count(pv) from PageView pv where pv.createdAt >= :createdAt group by pv.path order by count(pv) desc")
    List<Object[]> findPopularPagesByCreatedAtGreaterThanEqual(LocalDateTime createdAt, Pageable pageable);
}
