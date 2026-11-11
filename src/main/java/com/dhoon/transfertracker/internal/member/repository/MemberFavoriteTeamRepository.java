package com.dhoon.transfertracker.internal.member.repository;

import com.dhoon.transfertracker.internal.member.domain.MemberFavoriteTeam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberFavoriteTeamRepository extends JpaRepository<MemberFavoriteTeam, Long> {

    Optional<MemberFavoriteTeam> findByMemberIdAndTeamId(Long memberId, Long teamId);

    List<MemberFavoriteTeam> findAllByMemberId(Long memberId);

    List<MemberFavoriteTeam> findAllByTeamId(Long teamId);
}
