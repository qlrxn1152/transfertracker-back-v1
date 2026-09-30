package com.dhoon.transfertracker.internal.transferpost.service;

import com.dhoon.transfertracker.internal.transfer.dto.response.AllTransfersResponseDto;
import com.dhoon.transfertracker.internal.transfer.dto.response.PlayerTransfersResponseDto;

public interface TransferService {

    PlayerTransfersResponseDto getPlayerTransfers(Long playerId);

    AllTransfersResponseDto getTransfers(int page);

    AllTransfersResponseDto getTeamTransfers(Long teamId);



}
