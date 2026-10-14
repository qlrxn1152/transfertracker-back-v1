package com.dhoon.transfertracker.internal.transfer.query.controller;

import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.transfer.query.dto.response.AllTransfersResponseDto;
import com.dhoon.transfertracker.internal.transfer.query.dto.response.PlayerTransfersResponseDto;
import com.dhoon.transfertracker.internal.transfer.query.dto.response.TransferDetailResponseDto;
import com.dhoon.transfertracker.internal.transfer.query.service.impl.TransferQueryOrchestrationService;
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
public class TransferQueryController {

    private final TransferQueryOrchestrationService transferQueryOrchestrationService;

    @GetMapping("/api/player/transfer/{playerId}")
    public ResponseEntity<PlayerTransfersResponseDto> getPlayerTransfers(@PathVariable Long playerId) {
        PlayerTransfersResponseDto response = transferQueryOrchestrationService.getPlayerTransfers(playerId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/transfers")
    public ResponseEntity<AllTransfersResponseDto> getTransfers(
            @Min(0) @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "") String keyWord, @RequestParam(required = false) LeagueCode leagueCode, @RequestParam(required = false) Long teamId
    ) {
        AllTransfersResponseDto response = transferQueryOrchestrationService.getTransfers(page, keyWord, leagueCode, teamId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/transfers/{transferId}")
    public ResponseEntity<TransferDetailResponseDto> getTransfer(@PathVariable Long transferId) {
        TransferDetailResponseDto response = transferQueryOrchestrationService.getTransfer(transferId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/transfers/team/{teamId}")
    public ResponseEntity<AllTransfersResponseDto> getTeamTransfers(@PathVariable Long teamId, @Min(0) @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "") String keyWord) {
        AllTransfersResponseDto response = transferQueryOrchestrationService.getTeamTransfers(teamId, page, keyWord);

        return ResponseEntity.ok(response);
    }

}
