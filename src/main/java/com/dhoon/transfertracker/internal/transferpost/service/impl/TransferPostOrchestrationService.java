package com.dhoon.transfertracker.internal.transferpost.service.impl;

import com.dhoon.transfertracker.external.openai.dto.response.TranslationBatchResult;
import com.dhoon.transfertracker.internal.team.exception.InvalidTeamIdException;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostsResponseDto;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostDetailResponseDto;
import com.dhoon.transfertracker.internal.transferpost.exception.InvalidPostIdException;
import com.dhoon.transfertracker.internal.transferpost.exception.InvalidSourcerException;
import com.dhoon.transfertracker.internal.transferpost.service.TransferPostService;
import com.dhoon.transfertracker.internal.transferpost.service.translation.TransferPostTranslationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransferPostOrchestrationService {

    private final TransferPostService transferPostTxService;
    private final TransferPostTranslationServiceImpl translationService;

    public TransferPostsResponseDto getSourcerAllPosts(TransferPostSource sourcer) {
        validateSourcerIsNotNull(sourcer);

        return transferPostTxService.getSourcerAllPosts(sourcer);
    }


    public TransferPostsResponseDto getTeamTransferPosts(Long teamId) {
        validateTeamIdIsNotNull(teamId);

        return transferPostTxService.getTeamTransferPosts(teamId);
    }

    public TransferPostsResponseDto getAllTransferPosts() {
        return transferPostTxService.getAllTransferPosts();
    }

    public TransferPostDetailResponseDto getTransferPost(Long postId) {
        validatePostIdIsNotNull(postId);

        return transferPostTxService.getTransferPost(postId);
    }



    public String postAssignTeam() {
        return transferPostTxService.postAssignTeam();
    }

    public TranslationBatchResult translate() {
        return translationService.translate();
    }




    private void validateSourcerIsNotNull(TransferPostSource sourcer) {
        if (sourcer == null) {
            throw new InvalidSourcerException();
        }
    }

    private void validatePostIdIsNotNull(Long postId) {
        if (postId == null) {
            throw new InvalidPostIdException();
        }
    }

    private void validateTeamIdIsNotNull(Long teamId) {
        if (teamId == null) {
            throw new InvalidTeamIdException();
        }
    }

















}
