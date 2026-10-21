package com.dhoon.transfertracker.internal.transfer.query.service.impl;

import com.dhoon.transfertracker.internal.player.domain.Player;
import com.dhoon.transfertracker.internal.player.exception.NotFoundPlayerException;
import com.dhoon.transfertracker.internal.player.repository.PlayerRepository;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.transfer.domain.Transfer;
import com.dhoon.transfertracker.internal.transfer.query.dto.response.AllTransfersResponseDto;
import com.dhoon.transfertracker.internal.transfer.query.dto.response.PlayerTransferItemResponseDto;
import com.dhoon.transfertracker.internal.transfer.query.dto.response.PlayerTransfersResponseDto;
import com.dhoon.transfertracker.internal.transfer.query.dto.response.TransferDetailResponseDto;
import com.dhoon.transfertracker.internal.transfer.query.dto.response.TransferResponseDto;
import com.dhoon.transfertracker.internal.transfer.query.projection.TransferSearchProjection;
import com.dhoon.transfertracker.internal.transfer.query.repository.TransferQueryRepository;
import com.dhoon.transfertracker.internal.transfer.repository.TransferRepository;
import com.dhoon.transfertracker.internal.transfer.query.service.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Transactional
@Service
public class TransferQueryTxService implements TransferService {

    private final TransferRepository transferRepository;
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;

    private final TransferQueryRepository transferQueryRepository;


    @Override
    @Transactional(readOnly = true)
    public PlayerTransfersResponseDto getPlayerTransfers(Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(NotFoundPlayerException::new);

        List<PlayerTransferItemResponseDto> playerTransfers = transferRepository.findAllByPlayerId(playerId)
                .stream()
                .map(PlayerTransferItemResponseDto::of)
                .toList();

        return PlayerTransfersResponseDto.of(player, playerTransfers);
    }



    @Override
    @Transactional(readOnly = true)
    public AllTransfersResponseDto getTransfers(Pageable pageable, String keyWord, LeagueCode leagueCode, Long teamId) {

        Page<Transfer> transferPage = transferQueryRepository.findTransfers(teamId, leagueCode, keyWord, pageable);

        List<TransferResponseDto> transfers = transferPage.getContent()
                .stream()
                .map(TransferResponseDto::of)
                .toList();

        return AllTransfersResponseDto.of(transfers, transferPage.hasNext(), transferPage.hasPrevious());
    }












    @Override
    @Transactional(readOnly = true)
    public AllTransfersResponseDto getTeamTransfers(Long teamId, Pageable pageable, String keyWord) {
        teamRepository.findById(teamId).orElseThrow(NotFoundTeamException::new); // 쿼리 1번.

        Page<Transfer> transferPage = transferQueryRepository.findTransfers(teamId, null, keyWord, pageable);

        List<TransferResponseDto> transfers = transferPage.getContent()
                .stream()
                .map(TransferResponseDto::of)
                .toList();

        return AllTransfersResponseDto.of(transfers, transferPage.hasNext(), transferPage.hasPrevious());
    }


    @Override
    @Transactional(readOnly = true)
    public TransferDetailResponseDto getTransfer(Long transferId) {
        Transfer transfer = transferRepository.findByIdWithLazyEntities(transferId)
                .orElseThrow(() -> new IllegalArgumentException("이적 기록을 찾을 수 없습니다."));

        return TransferDetailResponseDto.of(TransferResponseDto.of(transfer));
    }

}
