package com.dhoon.transfertracker.internal.post.repository;

import com.dhoon.transfertracker.internal.post.domain.PostTeam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PostTeamRepository extends JpaRepository<PostTeam, Long> {

    @Query("""
            select pt from PostTeam pt
            join fetch pt.team
            join fetch pt.post
            where pt.team.id = :teamId
            order by pt.post.contentCreatedAt desc, pt.post.id desc
            """)
    Page<PostTeam> findAllByTeamId(Long teamId, Pageable pageable);

    boolean existsByPostId(Long postId);

    List<PostTeam> findAllByPostId(Long postId);

    Optional<PostTeam> findByTeamIdAndPostId(Long teamId, Long postId);
}
