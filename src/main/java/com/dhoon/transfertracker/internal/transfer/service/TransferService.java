package com.dhoon.transfertracker.internal.transfer.service;

import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.transfer.dto.response.AllTransfersResponseDto;
import com.dhoon.transfertracker.internal.transfer.dto.response.PlayerTransfersResponseDto;

public interface TransferService {

    PlayerTransfersResponseDto getPlayerTransfers(Long playerId);

    AllTransfersResponseDto getTransfers(int page, String keyWord, LeagueCode leagueCode, Long teamId);

    AllTransfersResponseDto getTeamTransfers(Long teamId, int page, String keyWord);




}
