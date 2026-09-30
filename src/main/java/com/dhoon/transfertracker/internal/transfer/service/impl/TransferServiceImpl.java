package com.dhoon.transfertracker.internal.transfer.service.impl;

import com.dhoon.transfertracker.internal.player.dto.response.PlayerItemResponseDto;
import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
import com.dhoon.transfertracker.internal.transfer.dto.response.AllTransfersResponseDto;
import com.dhoon.transfertracker.internal.transfer.dto.response.PlayerTransferItemResponseDto;
import com.dhoon.transfertracker.internal.transfer.dto.response.PlayerTransfersResponseDto;
import com.dhoon.transfertracker.internal.transfer.dto.response.TransferResponseDto;
import com.dhoon.transfertracker.internal.transfer.repository.TransferRepository;
import com.dhoon.transfertracker.internal.player.service.PlayerService;
import com.dhoon.transfertracker.internal.transferpost.service.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
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
    public AllTransfersResponseDto getTransfers(int page) {
        Slice<Transfer> transferSlice = transferRepository.findAllTransferWithLazyEntities(
                PageRequest.of(
                        page,
                        20,
                        Sort.by(Sort.Order.desc("transferDate"), Sort.Order.desc("id")))
        );

        List<TransferResponseDto> transfers = transferSlice.getContent()
                .stream()
                .map(TransferResponseDto::of)
                .toList();

        return AllTransfersResponseDto.of(transfers, transferSlice.hasNext(), transferSlice.hasPrevious());
    }

    @Override
    @Transactional(readOnly = true)
    public AllTransfersResponseDto getTeamTransfers(Long teamId) {
        return null;
    }




}
