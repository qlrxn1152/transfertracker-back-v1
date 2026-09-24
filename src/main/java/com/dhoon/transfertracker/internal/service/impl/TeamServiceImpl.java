package com.dhoon.transfertracker.internal.service.impl;

import com.dhoon.transfertracker.external.football.dto.team.TeamInfoResponseDto;
import com.dhoon.transfertracker.internal.domain.Team;
import com.dhoon.transfertracker.internal.dto.team.TeamItemResponseDto;
import com.dhoon.transfertracker.internal.dto.team.TeamsResponseDto;
import com.dhoon.transfertracker.internal.repository.TeamRepository;
import com.dhoon.transfertracker.internal.service.TeamService;
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


}
