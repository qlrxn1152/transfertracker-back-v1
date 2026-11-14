package com.dhoon.transfertracker.internal.transferpost.command.service.impl;

import com.dhoon.transfertracker.external.openai.client.OpenAiClient;
import com.dhoon.transfertracker.internal.transferpost.command.dto.TeamAssignPostsResult;
import com.dhoon.transfertracker.internal.transferpost.command.dto.TeamAssignPostsTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransferPostCommandOrchestrationService {

    private final TransferPostCommandTxService txService;
    private final OpenAiClient openAiClient;

    public String postAssignTeam() {
        TeamAssignPostsTarget targetPosts = txService.findTargetTeams();

        TeamAssignPostsResult result = openAiClient.classifyTeams(targetPosts);

        return txService.postAssignTeam(result);
    }


}
