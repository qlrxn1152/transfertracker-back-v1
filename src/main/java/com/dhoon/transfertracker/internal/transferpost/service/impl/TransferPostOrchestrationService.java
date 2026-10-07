package com.dhoon.transfertracker.internal.transferpost.service.impl;

import com.dhoon.transfertracker.external.openai.dto.response.TranslationBatchResult;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.exception.InvalidLeagueCodeValueException;
import com.dhoon.transfertracker.internal.team.exception.InvalidTeamIdException;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.transfer.dto.response.TransferPostsResponseDto;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.exception.InvalidSourcerException;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostRepository;
import com.dhoon.transfertracker.internal.transferpost.service.TransferPostService;
import com.dhoon.transfertracker.internal.transferpost.service.translation.TransferPostTranslationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransferPostOrchestrationService {

    private final TransferPostService transferPostTxService;
    private final TransferPostTranslationServiceImpl translationService;

    public TransferPostsResponseDto getSourcerAllPosts(TransferPostSource sourcer) {
        if (sourcer == null) {
            throw new InvalidSourcerException();
        }

        return transferPostTxService.getSourcerAllPosts(sourcer);
    }

    public TransferPostsResponseDto getTeamTransferPosts(Long teamId) {
        if (teamId == null) {
            throw new InvalidTeamIdException();
        }

        return transferPostTxService.getTeamTransferPosts(teamId);
    }

    public TransferPostsResponseDto getAllTransferPosts() {
        return transferPostTxService.getAllTransferPosts();
    }

    public String postAssignTeam() {
        return transferPostTxService.postAssignTeam();
    }

    public TranslationBatchResult translate() {
        return translationService.translate();
    }











}
