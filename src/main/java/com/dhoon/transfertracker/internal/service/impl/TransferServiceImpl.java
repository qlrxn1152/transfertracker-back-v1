package com.dhoon.transfertracker.internal.service.impl;

import com.dhoon.transfertracker.external.football.client.ApiFootballClient;
import com.dhoon.transfertracker.external.football.dto.playertransfer.PlayerTransferInfoResponseDto;
import com.dhoon.transfertracker.internal.domain.Transfer;
import com.dhoon.transfertracker.internal.dto.response.PlayerTransferItemResponseDto;
import com.dhoon.transfertracker.internal.dto.response.PlayerTransfersResponseDto;
import com.dhoon.transfertracker.internal.repository.TransferRepository;
import com.dhoon.transfertracker.internal.service.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final TransferRepository transferRepository;

    @Override
    public PlayerTransfersResponseDto getPlayerTransfer(Long playerId) {
        List<Transfer> transfers = transferRepository.findAllByPlayerId(playerId);

        List<PlayerTransferItemResponseDto> playerTransfers = transfers.stream()
                .map(PlayerTransferItemResponseDto::of)
                .toList();

        String playerName = transfers.getFirst().getPlayer().getPlayerName();

        return PlayerTransfersResponseDto.of(playerName, playerTransfers);
    }
}
