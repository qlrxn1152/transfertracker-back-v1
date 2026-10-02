package com.dhoon.transfertracker.internal.transfer.service.impl;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.dto.response.PlayerItemResponseDto;
import com.dhoon.transfertracker.internal.player.exception.InvalidPlayerIdException;
import com.dhoon.transfertracker.internal.player.exception.InvalidPlayerSearchPageValueException;
import com.dhoon.transfertracker.internal.team.service.TeamService;
import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
import com.dhoon.transfertracker.internal.transfer.dto.response.AllTransfersResponseDto;
import com.dhoon.transfertracker.internal.transfer.dto.response.PlayerTransferItemResponseDto;
import com.dhoon.transfertracker.internal.transfer.dto.response.PlayerTransfersResponseDto;
import com.dhoon.transfertracker.internal.transfer.dto.response.TransferResponseDto;
import com.dhoon.transfertracker.internal.transfer.exception.InvalidTeamIdException;
import com.dhoon.transfertracker.internal.transfer.exception.InvalidTransferSearchPageValueException;
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
    private final TeamService teamService;

    @Override
    @Transactional(readOnly = true)
    public PlayerTransfersResponseDto getPlayerTransfers(Long playerId) {

        if (playerId == null) {
            throw new InvalidPlayerIdException();
        }

        PlayerItemResponseDto player = playerService.getPlayer(playerId);

        List<PlayerTransferItemResponseDto> playerTransfers = transferRepository.findAllByPlayerId(playerId)
                .stream()
                .map(PlayerTransferItemResponseDto::of)
                .toList();

        return PlayerTransfersResponseDto.of(player.getPlayerName(), playerTransfers);
    }

    @Override
    @Transactional(readOnly = true)
    public AllTransfersResponseDto getTransfers(int page, String keyWord) {

        if (page < 0) {
            throw new InvalidTransferSearchPageValueException();
        }

        PageRequest pageable = PageRequest.of(
                page,
                50,
                Sort.by(Sort.Order.desc("transferDate"), Sort.Order.desc("id")));

        String normalizedKeyWord = keyWord == null ? "" : keyWord.trim();
        Slice<Transfer> transferSlice;

        if ( normalizedKeyWord.isBlank() ) {
            transferSlice = transferRepository.findAllTransferWithLazyEntities(pageable);
        }
        else {
            transferSlice = transferRepository.findByPlayerNameContainingIgnoreCaseWithLazyEntities(normalizedKeyWord, pageable);
        }

        List<TransferResponseDto> transfers = transferSlice.getContent()
                .stream()
                .map(TransferResponseDto::of)
                .toList();

        return AllTransfersResponseDto.of(transfers, transferSlice.hasNext(), transferSlice.hasPrevious());
    }


    @Override
    @Transactional(readOnly = true)
    public AllTransfersResponseDto getTeamTransfers(Long teamId, int page, String keyWord) {
        if (page < 0) {
            throw new InvalidTransferSearchPageValueException();
        }

        if ( teamId == null) {
            throw new InvalidTeamIdException();
        }



        teamService.getTeam(teamId);



        PageRequest pageable = PageRequest.of(
                page,
                50,
                Sort.by(Sort.Order.desc("transferDate"), Sort.Order.desc("id"))
        );

        String normalizedKeyWord = keyWord == null ? "" : keyWord.trim();

        Slice<Transfer> transferSlice;

        if ( normalizedKeyWord.isBlank() ) {
            transferSlice = transferRepository.findAllByTeamIdWithLazy(teamId, pageable);
        }
        else {
            transferSlice = transferRepository.findAllByTeamIdWithLazyAndKeyWord(teamId, normalizedKeyWord, pageable);
        }

        List<TransferResponseDto> transfers = transferSlice.getContent()
                .stream()
                .map(TransferResponseDto::of)
                .toList();

        return AllTransfersResponseDto.of(transfers, transferSlice.hasNext(), transferSlice.hasPrevious());
    }




}
