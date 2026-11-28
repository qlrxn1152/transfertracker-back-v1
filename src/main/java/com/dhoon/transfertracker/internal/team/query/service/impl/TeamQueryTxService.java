package com.dhoon.transfertracker.internal.team.query.service.impl;

import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.TeamCollectionLeague;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.query.dto.response.TeamItemResponseDto;
import com.dhoon.transfertracker.internal.team.query.dto.response.TeamPageInfoForTeamResponseDto;
import com.dhoon.transfertracker.internal.team.query.dto.response.TeamPageInfosResponseDto;
import com.dhoon.transfertracker.internal.team.query.dto.response.TeamsResponseDto;
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
        Team team = teamRepository.findById(teamId)
                .orElseThrow(NotFoundTeamException::new);

        return TeamItemResponseDto.of(team);
    }

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

    public TeamsResponseDto getCollectionLeagueTeams(TeamCollectionLeague leagueCode) {
        List<TeamItemResponseDto> teams = teamRepository.findAllByCollectionLeagueCode(leagueCode)
                .stream()
                .map(TeamItemResponseDto::of)
                .toList();

        return TeamsResponseDto.of(teams);
    }

    public TeamPageInfosResponseDto getTeamInfo(Long teamId, Pageable pageable) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(NotFoundTeamException::new);

        long teamPlayerCount = teamPlayerRepository.countByTeamId(teamId);

        List<TeamPlayerItemResponseDto> playersData = teamPlayerRepository.findAllByTeamIdWithLazyEntity(teamId)
                .stream()
                .map(TeamPlayerItemResponseDto::of)
                .toList();

        Page<PostTeam> postPage = postTeamRepository.findAllByTeamId(teamId, pageable);

        long totalPostCount = postPage.getTotalElements();
        int totalPageCount = postPage.getTotalPages();
        boolean hasNext = postPage.hasNext();
        boolean hasPrevious = postPage.hasPrevious();

        List<PostItemResponseDto> postsData = postPage.getContent()
                .stream()
                .map(PostItemResponseDto::createTeamPost)
                .toList();

        TeamPageInfoForTeamResponseDto teams = TeamPageInfoForTeamResponseDto.of(team, teamPlayerCount);// 팀에 대한 정보 ... ( teamId, name, 속해있는 선수 숫자, 엠블렘 ... )

        TeamPlayersResponseDto players = TeamPlayersResponseDto.of(playersData);// 팀에 속한 선수들 정보 ( playerId, playerName, player 사진 )

        PostsResponseDto posts = PostsResponseDto.of(postsData, totalPostCount, totalPageCount, hasNext, hasPrevious);// 팀 게시물에 대한 정보 ( postId, externalPostId, content, source, postCreatedAt, 이적과 관련된건지 )

        return TeamPageInfosResponseDto.of(teams, players, posts);
    }

}
