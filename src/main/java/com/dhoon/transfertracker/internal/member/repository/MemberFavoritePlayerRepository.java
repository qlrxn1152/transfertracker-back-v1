package com.dhoon.transfertracker.internal.member.repository;

import com.dhoon.transfertracker.internal.member.domain.MemberFavoritePlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MemberFavoritePlayerRepository extends JpaRepository<MemberFavoritePlayer, Long> {

    Optional<MemberFavoritePlayer> findByMemberIdAndPlayerId(Long memberId, Long playerId);

    boolean existsByMemberIdAndPlayerId(Long memberId, Long playerId);

    List<MemberFavoritePlayer> findAllByMemberId(Long memberId);

    @Query("""
    SELECT mf FROM MemberFavoritePlayer mf
    JOIN FETCH mf.member
    JOIN FETCH mf.player
    WHERE mf.player.id = :playerId
    """)
    List<MemberFavoritePlayer> findAllByPlayerId(Long playerId);
}
