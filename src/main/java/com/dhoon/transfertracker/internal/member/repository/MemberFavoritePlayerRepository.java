package com.dhoon.transfertracker.internal.member.repository;

import com.dhoon.transfertracker.internal.member.domain.MemberFavoritePlayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberFavoritePlayerRepository extends JpaRepository<MemberFavoritePlayer, Long> {

    Optional<MemberFavoritePlayer> findByMemberIdAndPlayerId(Long memberId, Long playerId);

    boolean existsByMemberIdAndPlayerId(Long memberId, Long playerId);

    List<MemberFavoritePlayer> findAllByMemberId(Long memberId);

    List<MemberFavoritePlayer> findAllByPlayerId(Long playerId);
}
