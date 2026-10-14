package com.dhoon.transfertracker.internal.transfer.sync.controller;

import com.dhoon.transfertracker.external.football.dto.TeamTransfersSaveResponseDto;
import com.dhoon.transfertracker.internal.transfer.sync.service.impl.TransferSyncOrchestrationService;
import com.dhoon.transfertracker.internal.transfer.query.dto.response.PlayerTransfersResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class TransferSyncController {

    private final TransferSyncOrchestrationService transferSyncOrchestrationService;

    @PostMapping("/external/api/transfer/{playerApiId}")
    public ResponseEntity<PlayerTransfersResponseDto> savePlayerTransfer(@PathVariable Long playerApiId) {
        PlayerTransfersResponseDto response = transferSyncOrchestrationService.syncPlayerTransfers(playerApiId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/external/api/transfers/team/{teamApiId}")
    public ResponseEntity<TeamTransfersSaveResponseDto> saveTeamTransfers(@PathVariable Long teamApiId) {
        TeamTransfersSaveResponseDto response = transferSyncOrchestrationService.syncTeamTransfers(teamApiId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
