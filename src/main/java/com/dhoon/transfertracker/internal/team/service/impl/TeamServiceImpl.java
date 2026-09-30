package com.dhoon.transfertracker.internal.team.service.impl;

import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.dto.response.TeamItemResponseDto;
import com.dhoon.transfertracker.internal.team.dto.response.TeamPageInfoForTeamResponseDto;
import com.dhoon.transfertracker.internal.team.dto.response.TeamPageInfosResponseDto;
import com.dhoon.transfertracker.internal.team.dto.response.TeamsResponseDto;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.team.service.TeamService;
import com.dhoon.transfertracker.internal.teamplayer.dto.response.TeamPlayerItemResponseDto;
import com.dhoon.transfertracker.internal.teamplayer.dto.response.TeamPlayersResponseDto;
import com.dhoon.transfertracker.internal.teamplayer.repository.TeamPlayerRepository;
import com.dhoon.transfertracker.internal.transfer.dto.response.TransferPostsResponseDto;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostItemResponseDto;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;
    private final TeamPlayerRepository teamPlayerRepository;
    private final TransferPostRepository transferPostRepository;

    @Override
    @Transactional(readOnly = true)
    public TeamItemResponseDto getTeam(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow();

        return TeamItemResponseDto.of(team);
    }

    @Override
    @Transactional(readOnly = true)
    public TeamsResponseDto getTeams() {
        List<TeamItemResponseDto> teamItems = teamRepository.findAll().stream()
                .map(TeamItemResponseDto::of)
                .toList();

        return TeamsResponseDto.of(teamItems);
    }

    @Override
    @Transactional(readOnly = true)
    public TeamsResponseDto getLeagueTeams(String leagueCode) {
        List<TeamItemResponseDto> leagueTeams = teamRepository.findAllByLeagueCode(LeagueCode.valueOf(leagueCode.toUpperCase())).stream()
                .map(TeamItemResponseDto::of)
                .toList();


        return TeamsResponseDto.of(leagueTeams);
    }

    @Override
    @Transactional(readOnly = true)
    public TeamPageInfosResponseDto getTeamInfo(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow();
        long teamPlayerCount = teamPlayerRepository.countByTeamId(teamId);

        List<TeamPlayerItemResponseDto> playersData = teamPlayerRepository.findAllByTeamIdWithLazyEntity(teamId)
                .stream()
                .map(TeamPlayerItemResponseDto::of)
                .toList();

        List<TransferPostItemResponseDto> postsData = transferPostRepository.findAllByTeamId(teamId)
                .stream()
                .map(TransferPostItemResponseDto::of)
                .toList();



        // 팀 관련 게시물 소식들 , 팀에 속한 선수들, 팀에 대한 정보..

        TeamPageInfoForTeamResponseDto teams = TeamPageInfoForTeamResponseDto.of(team, teamPlayerCount);// 팀에 대한 정보 ... ( teamId, name, 속해있는 선수 숫자, 엠블렘 ... )

        TeamPlayersResponseDto players = TeamPlayersResponseDto.of(playersData);// 팀에 속한 선수들 정보 ( playerId, playerName, player 사진 )

        TransferPostsResponseDto posts = TransferPostsResponseDto.of(postsData);// 팀 게시물에 대한 정보 ( postId, externalPostId, content, source, postCreatedAt, 이적과 관련된건지 )

        return TeamPageInfosResponseDto.of(teams, players, posts);
    }


}
