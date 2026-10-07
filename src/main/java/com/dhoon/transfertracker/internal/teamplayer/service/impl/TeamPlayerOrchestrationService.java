package com.dhoon.transfertracker.internal.teamplayer.service.impl;

import com.dhoon.transfertracker.internal.team.exception.InvalidTeamIdException;
import com.dhoon.transfertracker.internal.teamplayer.dto.response.TeamPlayersResponseDto;
import com.dhoon.transfertracker.internal.teamplayer.service.TeamPlayerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TeamPlayerOrchestrationService {

    private final TeamPlayerService teamPlayerTxService;

    // 팀에 플레이어가 없는경우 빈 리스트를 반환.
    public TeamPlayersResponseDto getTeamPlayers(Long teamId) {
        validateTeamId(teamId);

        return teamPlayerTxService.getTeamPlayers(teamId);
    }

    private void validateTeamId(Long teamId) {
        if (teamId == null) {
            throw new InvalidTeamIdException();
        }
    }
}
