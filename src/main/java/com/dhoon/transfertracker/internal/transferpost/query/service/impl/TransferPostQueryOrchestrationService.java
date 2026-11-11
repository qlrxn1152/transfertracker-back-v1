package com.dhoon.transfertracker.internal.transferpost.query.service.impl;

import com.dhoon.transfertracker.internal.team.exception.InvalidTeamIdException;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostDetailResponseDto;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostsPageResponseDto;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostsResponseDto;
import com.dhoon.transfertracker.internal.transferpost.exception.InvalidPostIdException;
import com.dhoon.transfertracker.internal.transferpost.exception.InvalidSourcerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransferPostQueryOrchestrationService {

    private final TransferPostQueryTxService txService;

    public TransferPostsPageResponseDto getSourcerAllPosts(TransferPostSource sourcer, int page) {
        validateSourcerIsNotNull(sourcer);

        if (page < 0) {
            throw new IllegalArgumentException("페이지 값은 0보다 작을 수 없습니다.");
        }

        // 가장 최근에 저장된 게시물을 20개 단위로.
        Pageable pageable = PageRequest.of(
                page,
                20,
                Sort.by(
                        Sort.Order.desc("contentCreatedAt"),
                        Sort.Order.desc("id")
                )
        );

        return txService.getSourcerAllPosts(sourcer, pageable);
    }

    public TransferPostsResponseDto getAllTransferPosts(int page) {

        if (page < 0) {
            throw new IllegalArgumentException("페이지 값은 0보다 작을 수 없습니다.");
        }

        // 가장 최근에 저장된 게시물을 20개 단위로.
        Pageable pageable = PageRequest.of(
                page,
                20,
                Sort.by(
                        Sort.Order.desc("contentCreatedAt"),
                        Sort.Order.desc("id")
                )
        );

        return txService.getAllTransferPosts(pageable);

    }

    public TransferPostsResponseDto getTeamTransferPosts(Long teamId, int page) {
        validateTeamIdIsNotNull(teamId);

        if (page < 0) {
            throw new IllegalArgumentException("페이지 값은 0보다 작을 수 없습니다.");
        }

        // 가장 최근에 저장된 게시물을 20개 단위로.
        Pageable pageable = PageRequest.of(
                page,
                20,
                Sort.by(
                        Sort.Order.desc("post.contentCreatedAt"),
                        Sort.Order.desc("id")
                )
        );


        return txService.getTeamTransferPosts(teamId, pageable);
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
