package com.dhoon.transfertracker.internal.transferpost.query.service.impl;

import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostDetailResponseDto;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostItemResponseDto;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostsResponseDto;
import com.dhoon.transfertracker.internal.transferpost.exception.InvalidPostIdException;
import com.dhoon.transfertracker.internal.transferpost.query.service.TransferPostQueryService;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class TransferPostQueryTransactionService implements TransferPostQueryService {

    private final TransferPostRepository transferPostRepository;
    private final TeamRepository teamRepository;


    @Override
    @Transactional(readOnly = true)
    public TransferPostsResponseDto getSourcerAllPosts(TransferPostSource sourcer) {
        List<TransferPostItemResponseDto> posts = transferPostRepository.findAllBySourcer(sourcer)
                .stream()
                .map(TransferPostItemResponseDto::of)
                .toList();

        return TransferPostsResponseDto.of(posts);
    }

    @Override
    @Transactional(readOnly = true)
    public TransferPostsResponseDto getTeamTransferPosts(Long teamId) {
        teamRepository.findById(teamId)
                .orElseThrow(NotFoundTeamException::new);


        List<TransferPostItemResponseDto> teamPosts = transferPostRepository.findAllByTeamId(teamId)
                .stream()
                .map(TransferPostItemResponseDto::of)
                .toList();

        return TransferPostsResponseDto.of(teamPosts);
    }


    @Override
    @Transactional(readOnly = true)
    public TransferPostsResponseDto getAllTransferPosts() {
        List<TransferPostItemResponseDto> allPosts = transferPostRepository.findAll()
                .stream()
                .map(TransferPostItemResponseDto::of)
                .toList();

        return TransferPostsResponseDto.of(allPosts);
    }

    @Override
    @Transactional(readOnly = true)
    public TransferPostDetailResponseDto getTransferPost(Long postId) {
        TransferPost transferPost = transferPostRepository.findByIdWithTeam(postId)
                .orElseThrow(InvalidPostIdException::new);

        return TransferPostDetailResponseDto.of(TransferPostItemResponseDto.of(transferPost));
    }
}
