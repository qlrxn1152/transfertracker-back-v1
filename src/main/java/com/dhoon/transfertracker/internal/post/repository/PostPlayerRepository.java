package com.dhoon.transfertracker.internal.post.repository;

import com.dhoon.transfertracker.internal.post.domain.PostPlayer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PostPlayerRepository extends JpaRepository<PostPlayer, Long> {

    Optional<PostPlayer> findByPlayerIdAndPostId(Long playerId, Long postId);

    @Query("""
            select pp from PostPlayer pp
            join fetch pp.player
            join fetch pp.post
            where pp.player.id = :playerId
            order by pp.post.contentCreatedAt desc, pp.post.id desc
            """)
    Page<PostPlayer> findAllByPlayerId(Long playerId, Pageable pageable);
}
