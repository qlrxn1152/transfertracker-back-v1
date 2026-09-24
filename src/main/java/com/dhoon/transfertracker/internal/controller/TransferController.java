package com.dhoon.transfertracker.internal.controller;

import com.dhoon.transfertracker.internal.dto.response.AllTransfersResponseDto;
import com.dhoon.transfertracker.internal.dto.response.PlayerTransfersResponseDto;
import com.dhoon.transfertracker.internal.service.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @GetMapping("/api/player/transfer/{playerId}")
    public ResponseEntity<PlayerTransfersResponseDto> getPlayerTransfers(@PathVariable Long playerId) {
        PlayerTransfersResponseDto response = transferService.getPlayerTransfers(playerId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/transfers")
    public ResponseEntity<AllTransfersResponseDto> getTransfers() {
        AllTransfersResponseDto response = transferService.getTransfers();

        return ResponseEntity.ok(response);
    }
}
