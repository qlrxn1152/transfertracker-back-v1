package com.dhoon.transfertracker.internal.service.impl;

import com.dhoon.transfertracker.internal.domain.TeamPlayer;
import com.dhoon.transfertracker.internal.dto.response.PlayerItemResponseDto;
import com.dhoon.transfertracker.internal.dto.response.TeamPlayerItemResponseDto;
import com.dhoon.transfertracker.internal.dto.response.TeamPlayersResponseDto;
import com.dhoon.transfertracker.internal.repository.TeamPlayerRepository;
import com.dhoon.transfertracker.internal.service.TeamPlayerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Transactional
@RequiredArgsConstructor
@Service
public class TeamPlayerServiceImpl implements TeamPlayerService {

    private final TeamPlayerRepository teamPlayerRepository;

    @Override
    public TeamPlayersResponseDto getTeamPlayers(Long teamId) {

        List<TeamPlayerItemResponseDto> teamPlayers = teamPlayerRepository.findAllByTeamId(teamId)
                .stream()
                .map(TeamPlayerItemResponseDto::of)
                .toList();

        return TeamPlayersResponseDto.of(teamPlayers);
    }




}
