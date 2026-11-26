package com.dhoon.transfertracker.internal.post.repository;

import com.dhoon.transfertracker.internal.post.domain.PostTeam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PostTeamRepository extends JpaRepository<PostTeam, Long> {

    @Query(
            value = """
        select pt from PostTeam pt
        join fetch pt.team
        join fetch pt.post
        where pt.team.id = :teamId
        order by pt.postCreatedAt desc, fk(pt.post) desc
        """,
            countQuery = """
        select count(pt)
        from PostTeam pt
        where pt.team.id = :teamId
        """
    )
    Page<PostTeam> findAllByTeamId(Long teamId, Pageable pageable);


    boolean existsByPostId(Long postId);

    @Query("""
            select pt from PostTeam pt
            join fetch pt.team
            where pt.post.id = :postId
            order by pt.id asc
            """)
    List<PostTeam> findAllByPostId(Long postId);

    Optional<PostTeam> findByTeamIdAndPostId(Long teamId, Long postId);
}
