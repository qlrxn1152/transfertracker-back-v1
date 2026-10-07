package com.dhoon.transfertracker.internal.transferpost.service.impl;

import com.dhoon.transfertracker.external.openai.dto.response.TranslationBatchResult;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.exception.NotFoundTeamException;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.transfer.dto.response.TransferPostsResponseDto;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostItemResponseDto;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostRepository;
import com.dhoon.transfertracker.internal.transferpost.service.TransferPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class TransferPostTxService implements TransferPostService {

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
        // 팀은 있는게 맞지않을까?
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
    public String postAssignTeam() {
        List<TransferPost> teamNotAssignPosts = transferPostRepository.findAllByTeamIsNull();
        List<Team> allLeagueTeams = teamRepository.findAllByLeagueCodeIsNotNull();

        teamNotAssignPosts.forEach(post -> {
            String lowerContent = post.getContent().toLowerCase();

            allLeagueTeams.forEach(team -> {
                String lowerTeamName = team.getTeamName().toLowerCase();

                if (lowerContent.contains(lowerTeamName)) {
                    post.assignTeam(team);
                }

            });
        });

        return "OK";
    }
}
