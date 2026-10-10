package com.dhoon.transfertracker.internal.member.service.impl;

import com.dhoon.transfertracker.internal.member.domain.Member;
import com.dhoon.transfertracker.internal.member.domain.MemberFavoriteTeam;
import com.dhoon.transfertracker.internal.member.dto.request.MemberFavoriteTeamRegisterRequestDto;
import com.dhoon.transfertracker.internal.member.dto.response.MemberFavoriteTeamItemResponseDto;
import com.dhoon.transfertracker.internal.member.dto.response.MemberFavoriteTeamsResponseDto;
import com.dhoon.transfertracker.internal.member.exception.AlreadyRegisteredFavoriteTeamException;
import com.dhoon.transfertracker.internal.member.exception.InvalidFavoriteTeamRequestException;
import com.dhoon.transfertracker.internal.member.exception.NotFoundMemberException;
import com.dhoon.transfertracker.internal.member.repository.MemberFavoriteTeamRepository;
import com.dhoon.transfertracker.internal.member.repository.MemberRepository;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MemberFavoriteTeamService {

    private final MemberRepository memberRepository;
    private final TeamRepository teamRepository;
    private final MemberFavoriteTeamRepository memberFavoriteTeamRepository;

    public void registerFavoriteTeams(Long memberId, MemberFavoriteTeamRegisterRequestDto request) {

        validateDuplicateTeamIds(request.getTeamIds());

        Member member = memberRepository.findById(memberId)
                .orElseThrow(NotFoundMemberException::new);

        List<Team> teams = teamRepository.findAllById(request.getTeamIds());

        if (teams.size() != request.getTeamIds().size()) {
            throw new NotFoundTeamException();
        }

        List<MemberFavoriteTeam> existingFavorites = memberFavoriteTeamRepository.findAllByMemberIdAndTeamIdIn(memberId, request.getTeamIds());

        if (!existingFavorites.isEmpty()) {
            throw new AlreadyRegisteredFavoriteTeamException();
        }

        List<MemberFavoriteTeam> favoriteTeams = teams.stream()
                .map(team -> MemberFavoriteTeam.of(member, team))
                .toList();

        memberFavoriteTeamRepository.saveAll(favoriteTeams);
    }

    public MemberFavoriteTeamsResponseDto getFavoriteTeams(Long memberId) {
        memberRepository.findById(memberId)
                .orElseThrow(NotFoundMemberException::new);

        List<MemberFavoriteTeamItemResponseDto> favorites = memberFavoriteTeamRepository.findAllWithTeamByMemberId(memberId)
                .stream()
                .map(MemberFavoriteTeamItemResponseDto::of)
                .toList();

        return MemberFavoriteTeamsResponseDto.of(favorites);
    }

    public void deleteFavoriteTeams(Long memberId, Long teamId) {
        memberRepository.findById(memberId)
                .orElseThrow(NotFoundMemberException::new);

        memberFavoriteTeamRepository.deleteByMemberIdAndTeamId(memberId, teamId);
    }












    private void validateDuplicateTeamIds(List<Long> teamIds) {
        Set<Long> teamIdsSet = new HashSet<>(teamIds);

        if ( teamIdsSet.size() != teamIds.size()) {
            throw new InvalidFavoriteTeamRequestException();
        }
    }

}
