package com.dhoon.transfertracker.internal.member.repository;

import com.dhoon.transfertracker.internal.member.domain.MemberFavoriteTeam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberFavoriteTeamRepository extends JpaRepository<MemberFavoriteTeam, Long> {

    List<MemberFavoriteTeam> findAllByMemberIdAndTeamIdIn(Long memberId, List<Long> teamIds);
}
