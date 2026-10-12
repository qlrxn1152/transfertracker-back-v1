package com.dhoon.transfertracker.etc.guide.repository;

import com.dhoon.transfertracker.etc.guide.domain.GuidePost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GuidePostRepository extends JpaRepository<GuidePost, Long> {

    List<GuidePost> findTop20ByActiveTrueOrderByCreatedAtDescIdDesc();

    Optional<GuidePost> findByIdAndActiveTrue(Long id);
}
