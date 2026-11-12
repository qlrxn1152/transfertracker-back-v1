package com.dhoon.transfertracker.internal.member.repository;

import com.dhoon.transfertracker.internal.member.domain.MemberFavoriteTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MemberFavoriteTeamRepository extends JpaRepository<MemberFavoriteTeam, Long> {

    Optional<MemberFavoriteTeam> findByMemberIdAndTeamId(Long memberId, Long teamId);

    List<MemberFavoriteTeam> findAllByMemberId(Long memberId);

    @Query("""
    SELECT mf FROM MemberFavoriteTeam mf
    join fetch mf.member
    WHERE mf.team.id = :teamId
    """)
    List<MemberFavoriteTeam> findAllByTeamId(Long teamId);
}
