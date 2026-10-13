package com.dhoon.transfertracker.internal.transferpost.query.service.impl;

import com.dhoon.transfertracker.internal.team.exception.InvalidTeamIdException;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostDetailResponseDto;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostsResponseDto;
import com.dhoon.transfertracker.internal.transferpost.exception.InvalidPostIdException;
import com.dhoon.transfertracker.internal.transferpost.exception.InvalidSourcerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransferPostQueryOrchestrationService {

    private final TransferPostQueryTxService txService;

    public TransferPostsResponseDto getSourcerAllPosts(TransferPostSource sourcer) {
        validateSourcerIsNotNull(sourcer);

        return txService.getSourcerAllPosts(sourcer);
    }


    public TransferPostsResponseDto getTeamTransferPosts(Long teamId) {
        validateTeamIdIsNotNull(teamId);

        return txService.getTeamTransferPosts(teamId);
    }

    public TransferPostsResponseDto getAllTransferPosts() {
        return txService.getAllTransferPosts();
    }

    public TransferPostDetailResponseDto getTransferPost(Long postId) {
        validatePostIdIsNotNull(postId);

        return txService.getTransferPost(postId);
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
