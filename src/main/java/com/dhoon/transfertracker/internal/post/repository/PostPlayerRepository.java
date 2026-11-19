package com.dhoon.transfertracker.internal.post.repository;

import com.dhoon.transfertracker.internal.post.domain.PostPlayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostPlayerRepository extends JpaRepository<PostPlayer, Long> {

    Optional<PostPlayer> findByPlayerIdAndPostId(Long playerId, Long postId);
}
