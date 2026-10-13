package com.dhoon.transfertracker.internal.transferpost.command.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransferPostCommandOrchestrationService {

    private final TransferPostCommandTxService txService;

    public String postAssignTeam() {
        return txService.postAssignTeam();
    }

}
