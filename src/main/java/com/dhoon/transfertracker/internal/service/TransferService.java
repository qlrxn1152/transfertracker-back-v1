package com.dhoon.transfertracker.internal.service;

import com.dhoon.transfertracker.internal.dto.response.AllTransfersResponseDto;
import com.dhoon.transfertracker.internal.dto.response.PlayerTransfersResponseDto;

public interface TransferService {

    PlayerTransfersResponseDto getPlayerTransfers(Long playerId);

    AllTransfersResponseDto getTransfers();

}
