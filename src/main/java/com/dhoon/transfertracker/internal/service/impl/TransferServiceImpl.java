package com.dhoon.transfertracker.internal.service.impl;

import com.dhoon.transfertracker.internal.dto.response.PlayerTransfersResponseDto;
import com.dhoon.transfertracker.internal.service.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional
@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {
    @Override
    public PlayerTransfersResponseDto getPlayerTransfer(Long playerId) {
        return null;
    }
}
