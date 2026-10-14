package com.dhoon.transfertracker.internal.transfer.query.service;

import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.transfer.query.dto.response.AllTransfersResponseDto;
import com.dhoon.transfertracker.internal.transfer.query.dto.response.PlayerTransfersResponseDto;
import com.dhoon.transfertracker.internal.transfer.query.dto.response.TransferDetailResponseDto;
import org.springframework.data.domain.Pageable;

public interface TransferService {

    PlayerTransfersResponseDto getPlayerTransfers(Long playerId);

    AllTransfersResponseDto getTransfers(Pageable pageable, String keyWord, LeagueCode leagueCode, Long teamId);

    AllTransfersResponseDto getTeamTransfers(Long teamId, Pageable pageable, String keyWord);

    TransferDetailResponseDto getTransfer(Long transferId);




}
