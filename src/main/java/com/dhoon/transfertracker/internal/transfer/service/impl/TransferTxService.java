package com.dhoon.transfertracker.internal.transfer.service.impl;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.dto.response.PlayerItemResponseDto;
import com.dhoon.transfertracker.internal.player.exception.NotFoundPlayerException;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.player.service.PlayerService;
import com.dhoon.transfertracker.internal.player.service.impl.PlayerOrchestrationService;
import com.dhoon.transfertracker.internal.player.service.impl.PlayerTxService;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
import com.dhoon.transfertracker.internal.transfer.dto.response.AllTransfersResponseDto;
import com.dhoon.transfertracker.internal.transfer.dto.response.PlayerTransferItemResponseDto;
import com.dhoon.transfertracker.internal.transfer.dto.response.PlayerTransfersResponseDto;
import com.dhoon.transfertracker.internal.transfer.dto.response.TransferResponseDto;
import com.dhoon.transfertracker.internal.transfer.repository.TransferRepository;
import com.dhoon.transfertracker.internal.transfer.service.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Transactional
@Service
public class TransferTxService implements TransferService {

    private final TransferRepository transferRepository;
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;

    @Override
    @Transactional(readOnly = true)
    public PlayerTransfersResponseDto getPlayerTransfers(Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(NotFoundPlayerException::new);

        List<PlayerTransferItemResponseDto> playerTransfers = transferRepository.findAllByPlayerId(playerId)
                .stream()
                .map(PlayerTransferItemResponseDto::of)
                .toList();

        return PlayerTransfersResponseDto.of(player.getPlayerName(), playerTransfers);
    }

    @Override
    public AllTransfersResponseDto getTransfers(Pageable pageable, String keyWord, LeagueCode leagueCode, Long teamId) {
        teamRepository.findById(teamId)
                .orElseThrow(NotFoundTeamException::new);

        Slice<Transfer> transferSlice = transferRepository.findTransfers(keyWord, pageable, leagueCode, teamId);

        List<TransferResponseDto> transfers = transferSlice.getContent()
                .stream()
                .map(TransferResponseDto::of)
                .toList();

        return AllTransfersResponseDto.of(transfers,transferSlice.hasNext(), transferSlice.hasPrevious());
    }

    @Override
    public AllTransfersResponseDto getTeamTransfers(Long teamId, Pageable pageable, String keyWord) {
        teamRepository.findById(teamId)
                .orElseThrow(NotFoundTeamException::new);

        Slice<Transfer> transferSlice;

        if (keyWord.isBlank()) {
            transferSlice = transferRepository.findAllByTeamIdWithLazy(teamId, pageable);
        }
        else {
            transferSlice = transferRepository.findAllByTeamIdWithLazyAndKeyWord(teamId, keyWord, pageable);
        }

        List<TransferResponseDto> transfers = transferSlice.getContent()
                .stream()
                .map(TransferResponseDto::of)
                .toList();

        return AllTransfersResponseDto.of(transfers,transferSlice.hasNext(), transferSlice.hasPrevious());
    }
}
