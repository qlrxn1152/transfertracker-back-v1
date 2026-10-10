package com.dhoon.transfertracker.internal.member.repository;

import com.dhoon.transfertracker.internal.member.domain.MemberFavoriteTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MemberFavoriteTeamRepository extends JpaRepository<MemberFavoriteTeam, Long> {

    List<MemberFavoriteTeam> findAllByMemberIdAndTeamIdIn(Long memberId, List<Long> teamIds);

    @Query("select mft from MemberFavoriteTeam mft join fetch mft.team where mft.member.id = :memberId")
    List<MemberFavoriteTeam> findAllWithTeamByMemberId(Long memberId);

    void deleteByMemberIdAndTeamId(Long memberId, Long teamId);
}
