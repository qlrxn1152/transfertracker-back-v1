package com.dhoon.transfertracker.internal.service.impl;

import com.dhoon.transfertracker.internal.domain.Transfer;
import com.dhoon.transfertracker.internal.dto.response.*;
import com.dhoon.transfertracker.internal.repository.TransferRepository;
import com.dhoon.transfertracker.internal.service.PlayerService;
import com.dhoon.transfertracker.internal.service.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Transactional
@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final TransferRepository transferRepository;
    private final PlayerService playerService;

    @Override
    @Transactional(readOnly = true)
    public PlayerTransfersResponseDto getPlayerTransfers(Long playerId) {

        PlayerItemResponseDto player = playerService.getPlayer(playerId);

        List<PlayerTransferItemResponseDto> playerTransfers = transferRepository.findAllByPlayerId(playerId)
                .stream()
                .map(PlayerTransferItemResponseDto::of)
                .toList();


        return PlayerTransfersResponseDto.of(player.getPlayerName(), playerTransfers);
    }

    @Override
    @Transactional(readOnly = true)
    public AllTransfersResponseDto getTransfers() {
        List<TransferResponseDto> transfers = transferRepository.findAllTransferWithLazyEntities()
                .stream()
                .map(TransferResponseDto::of)
                .toList();

        return AllTransfersResponseDto.of(transfers);
    }

    @Override
    public AllTransfersResponseDto getTeamTransfers(Long teamId) {
        return null;
    }




}
