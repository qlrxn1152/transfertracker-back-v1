package com.dhoon.transfertracker.internal.post.repository;

import com.dhoon.transfertracker.internal.post.domain.Post;
import com.dhoon.transfertracker.internal.post.domain.PostJournalist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    Optional<Post> findByExternalPostId(String externalPostId);

    boolean existsByExternalPostId(String externalPostId);

    Page<Post> findAllByJournalist(PostJournalist journalist, Pageable pageable);

    Page<Post> findAllByContentContainingIgnoreCase(String keyword, Pageable pageable);

    @Query("""
            select p
            from Post p
            where p.id > :afterPostId
              and not exists (select 1 from PostTeam pt where pt.post = p)
              and not exists (select 1 from PostPlayer pp where pp.post = p)
            order by p.id asc
            """)
    List<Post> findMappingBackfillTargets(@Param("afterPostId") Long afterPostId, Pageable pageable);
}
