package com.dhoon.transfertracker.internal.teamplayer.service.impl;

import com.dhoon.transfertracker.internal.teamplayer.dto.response.TeamPlayerItemResponseDto;
import com.dhoon.transfertracker.internal.teamplayer.dto.response.TeamPlayersResponseDto;
import com.dhoon.transfertracker.internal.teamplayer.repository.TeamPlayerRepository;
import com.dhoon.transfertracker.internal.teamplayer.service.TeamPlayerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Transactional
@RequiredArgsConstructor
@Service
public class TeamPlayerTxService implements TeamPlayerService {

    private final TeamPlayerRepository teamPlayerRepository;

    @Override
    public TeamPlayersResponseDto getTeamPlayers(Long teamId) {

        List<TeamPlayerItemResponseDto> teamPlayers = teamPlayerRepository.findAllByTeamIdWithLazyEntity(teamId)
                .stream()
                .map(TeamPlayerItemResponseDto::of)
                .toList();

        return TeamPlayersResponseDto.of(teamPlayers);

    }




}
