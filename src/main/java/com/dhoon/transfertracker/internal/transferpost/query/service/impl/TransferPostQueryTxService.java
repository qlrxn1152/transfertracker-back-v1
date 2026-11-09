package com.dhoon.transfertracker.internal.transferpost.query.service.impl;

import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostDetailResponseDto;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostItemResponseDto;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostsPageResponseDto;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostsResponseDto;
import com.dhoon.transfertracker.internal.transferpost.exception.InvalidPostIdException;
import com.dhoon.transfertracker.internal.transferpost.query.service.TransferPostQueryService;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostRepository;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostTeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TransferPostQueryTxService implements TransferPostQueryService {

    private final TransferPostRepository transferPostRepository;
    private final TeamRepository teamRepository;
    private final TransferPostTeamRepository transferPostTeamRepository;


    // 특정 기자의 모든 게시물들을 조회
    @Override
    public TransferPostsPageResponseDto getSourcerAllPosts(TransferPostSource sourcer, Pageable pageable) {

        Page<TransferPost> sourcerAllPostPage = transferPostRepository.findAllBySourcer(sourcer, pageable);

        long totalPostCount = sourcerAllPostPage.getTotalElements();
        int totalPageCount = sourcerAllPostPage.getTotalPages();
        boolean hasNext = sourcerAllPostPage.hasNext();
        boolean hasPrevious = sourcerAllPostPage.hasPrevious();

        List<TransferPostItemResponseDto> posts = sourcerAllPostPage.getContent()
                .stream()
                .map(TransferPostItemResponseDto::of)
                .toList();

        return TransferPostsPageResponseDto.of(posts, totalPostCount, totalPageCount, hasNext, hasPrevious);
    }



    @Override
    public TransferPostsResponseDto getTeamTransferPosts(Long teamId) {
        teamRepository.findById(teamId)
                .orElseThrow(NotFoundTeamException::new);


        List<TransferPostItemResponseDto> teamPosts = transferPostTeamRepository.findAllByTeamId(teamId)
                .stream()
                .map(TransferPostItemResponseDto::of)
                .toList();

        return TransferPostsResponseDto.of(teamPosts);
    }


    @Override
    public TransferPostsResponseDto getAllTransferPosts() {
        List<TransferPostItemResponseDto> allPosts = transferPostRepository.findAll()
                .stream()
                .map(TransferPostItemResponseDto::of)
                .toList();

        return TransferPostsResponseDto.of(allPosts);
    }

    @Override
    public TransferPostDetailResponseDto getTransferPost(Long postId) {
        TransferPost transferPost = transferPostRepository.findById(postId)
                .orElseThrow(InvalidPostIdException::new);

        return TransferPostDetailResponseDto.of(TransferPostItemResponseDto.of(transferPost));
    }
}
