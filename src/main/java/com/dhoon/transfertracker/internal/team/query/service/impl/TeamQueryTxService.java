package com.dhoon.transfertracker.internal.team.query.service.impl;

import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.exception.InvalidLeagueCodeValueException;
import com.dhoon.transfertracker.internal.team.exception.InvalidTeamIdException;
import com.dhoon.transfertracker.internal.team.query.dto.response.record.TeamDetailResponseDto;
import com.dhoon.transfertracker.internal.team.query.dto.response.record.TeamItemResponseDto;
import com.dhoon.transfertracker.internal.team.query.dto.response.record.TeamSummaryResponseDto;
import com.dhoon.transfertracker.internal.team.query.dto.response.record.TeamsResponseDto;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.teamplayer.query.dto.response.TeamPlayerItemResponseDto;
import com.dhoon.transfertracker.internal.teamplayer.query.dto.response.TeamPlayersResponseDto;
import com.dhoon.transfertracker.internal.teamplayer.repository.TeamPlayerRepository;
import com.dhoon.transfertracker.internal.post.domain.PostTeam;
import com.dhoon.transfertracker.internal.post.dto.response.PostItemResponseDto;
import com.dhoon.transfertracker.internal.post.dto.response.PostsResponseDto;
import com.dhoon.transfertracker.internal.post.repository.PostTeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
@Service
public class TeamQueryTxService {

    private final TeamRepository teamRepository;
    private final TeamPlayerRepository teamPlayerRepository;
    private final PostTeamRepository postTeamRepository;

    public TeamItemResponseDto getTeam(Long teamId) {
        Team team = findTeamOrThrow(teamId);
        return TeamItemResponseDto.of(team);
    }


    // 주요리그의 팀들만 가지고옴
    public TeamsResponseDto getTeams() {
        List<TeamItemResponseDto> teams = teamRepository.findAllByLeagueCodeIsNotNull()
                .stream()
                .map(TeamItemResponseDto::of)
                .toList();

        return TeamsResponseDto.of(teams);
    }



    public TeamsResponseDto getLeagueTeams(LeagueCode leagueCode) {
        List<TeamItemResponseDto> teams = teamRepository.findAllByLeagueCode(leagueCode)
                .stream()
                .map(TeamItemResponseDto::of)
                .toList();

        return TeamsResponseDto.of(teams);
    }



    public TeamDetailResponseDto getTeamInfo(Long teamId, Pageable pageable) {
        validatePageable(pageable);

        TeamSummaryResponseDto teamDetail = getTeamDetail(teamId);
        TeamPlayersResponseDto teamPlayerDetail = getTeamPlayersDetail(teamId);
        PostsResponseDto teamPostsDetail = getTeamPostsDetail(teamId, pageable);

        return TeamDetailResponseDto.of(teamDetail, teamPlayerDetail, teamPostsDetail);
    }

    // ==================================================================== //



    private TeamSummaryResponseDto getTeamDetail(Long teamId) {
        Team team = findTeamOrThrow(teamId);

        return TeamSummaryResponseDto.of(team);
    }

    private TeamPlayersResponseDto getTeamPlayersDetail(Long teamId) {
        List<TeamPlayerItemResponseDto> playersData = teamPlayerRepository.findAllByTeamIdWithLazyEntity(teamId)
                .stream()
                .map(TeamPlayerItemResponseDto::of)
                .toList();

        return TeamPlayersResponseDto.of(playersData);
    }

    private PostsResponseDto getTeamPostsDetail(Long teamId, Pageable pageable) {
        Page<PostItemResponseDto> postPage = postTeamRepository.findAllByTeamId(teamId, pageable)
                .map(PostItemResponseDto::createTeamPost);

        return PostsResponseDto.of(postPage);
    }


    // ==========================


    private Team findTeamOrThrow(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(NotFoundTeamException::new);
    }

    private void validatePageable(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            throw new IllegalArgumentException(
                    "유효한 페이징 정보가 필요합니다."
            );
        }
    }
}
