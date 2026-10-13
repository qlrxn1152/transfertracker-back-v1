package com.dhoon.transfertracker.internal.transferpost.command.controller;

import com.dhoon.transfertracker.internal.transferpost.command.service.TransferPostCommandOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TransferPostCommandController {

    private final TransferPostCommandOrchestrationService transferPostCommandOrchestrationService;



    @PostMapping("/external/api/post/assign")
    public ResponseEntity<String> postAssignTeam() {
        String response = transferPostCommandOrchestrationService.postAssignTeam();
        return ResponseEntity.ok(response);
    }


}
