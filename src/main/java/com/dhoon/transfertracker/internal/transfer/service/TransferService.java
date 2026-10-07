package com.dhoon.transfertracker.internal.transfer.service;

import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.transfer.dto.response.AllTransfersResponseDto;
import com.dhoon.transfertracker.internal.transfer.dto.response.PlayerTransfersResponseDto;
import org.springframework.data.domain.Pageable;

public interface TransferService {

    PlayerTransfersResponseDto getPlayerTransfers(Long playerId);

    AllTransfersResponseDto getTransfers(Pageable pageable, String keyWord, LeagueCode leagueCode, Long teamId);

    AllTransfersResponseDto getTeamTransfers(Long teamId, Pageable pageable, String keyWord);




}
