package com.dhoon.transfertracker.internal.transfer.controller;

import com.dhoon.transfertracker.internal.transfer.dto.response.AllTransfersResponseDto;
import com.dhoon.transfertracker.internal.transfer.dto.response.PlayerTransfersResponseDto;
import com.dhoon.transfertracker.internal.transferpost.service.TransferService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
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
    public ResponseEntity<AllTransfersResponseDto> getTransfers(@Min(0) @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "") String keyWord) {
        AllTransfersResponseDto response = transferService.getTransfers(page, keyWord);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/transfers/team/{teamId}")
    public ResponseEntity<AllTransfersResponseDto> getTeamTransfers(@PathVariable Long teamId) {
        AllTransfersResponseDto response = transferService.getTeamTransfers(teamId);

        return ResponseEntity.ok(response);
    }

}
