package com.dhoon.transfertracker.internal.service.impl;

import com.dhoon.transfertracker.external.football.client.ApiFootballClient;
import com.dhoon.transfertracker.external.football.dto.team.TeamInfoResponseDto;
import com.dhoon.transfertracker.internal.service.TeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TeamServiceImpl implements TeamService {

    private final ApiFootballClient apiFootballClient;

    @Override
    public TeamInfoResponseDto getTeam(Long teamId) {
        return apiFootballClient.saveTeam(teamId);
    }



}
