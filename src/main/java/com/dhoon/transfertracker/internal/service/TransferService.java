package com.dhoon.transfertracker.internal.service;

import com.dhoon.transfertracker.external.football.dto.playertransfer.PlayerTransferInfoResponseDto;
import com.dhoon.transfertracker.internal.dto.response.PlayerTransfersResponseDto;

public interface TransferService {

    PlayerTransfersResponseDto getPlayerTransfer(Long playerId);
}
