package com.dhoon.transfertracker.internal.team.service.impl;

import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.dto.response.TeamItemResponseDto;
import com.dhoon.transfertracker.internal.team.dto.response.TeamsResponseDto;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.team.service.TeamService;
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
        List<TeamItemResponseDto> leagueTeams = teamRepository.findAllByLeagueCode(Team.LeagueCode.valueOf(leagueCode.toUpperCase())).stream()
                .map(TeamItemResponseDto::of)
                .toList();


        return TeamsResponseDto.of(leagueTeams);


    }


}
