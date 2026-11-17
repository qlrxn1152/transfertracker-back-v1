package com.dhoon.transfertracker.internal.player.query.service.impl;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.exception.NotFoundTeamPlayerException;
import com.dhoon.transfertracker.internal.player.query.dto.response.PlayerItemResponseDto;
import com.dhoon.transfertracker.internal.player.query.dto.response.PlayersResponseDto;
import com.dhoon.transfertracker.internal.player.exception.NotFoundPlayerException;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.teamplayer.domain.TeamPlayer;
import com.dhoon.transfertracker.internal.teamplayer.repository.TeamPlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlayerQueryTxService {

    private final PlayerRepository playerRepository;
    private final TeamPlayerRepository teamPlayerRepository;

    public PlayerItemResponseDto getPlayer(Long playerId) {
        // 해당 선수가 존재하는지 확인
        Player player = playerRepository.findById(playerId)
                .orElseThrow(NotFoundPlayerException::new);

        // 해당 선수가 팀에 속한 선수인지 확인

        // 존재 -> PlayerItemResponseDto -> teamPlayer 객체대상
        // 존재 x -> PlayerItemResponseDto -> player 객체대상
        return teamPlayerRepository.findByPlayerId(playerId)
                .map(PlayerItemResponseDto::of) // teamPlayer
                .orElseGet(() -> PlayerItemResponseDto.of(player));
    }

    public PlayersResponseDto getPlayers(Pageable pageable, String keyWord, LeagueCode leagueCode, Long teamId) {

        Slice<TeamPlayer> playerSlice = teamPlayerRepository.findTeamPlayers(keyWord, pageable, leagueCode, teamId);

        List<PlayerItemResponseDto> players = contentToResponseDto(playerSlice);

        return PlayersResponseDto.of(players, playerSlice.hasNext(), playerSlice.hasPrevious());
    }

    private List<PlayerItemResponseDto> contentToResponseDto(Slice<TeamPlayer> playerSlice) {
        return playerSlice.getContent()
                .stream()
                .map(PlayerItemResponseDto::of)
                .toList();
    }


}
