package com.dhoon.transfertracker.internal.transferpost.service.impl;

import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostItemResponseDto;
import com.dhoon.transfertracker.internal.transfer.dto.response.TransferPostsResponseDto;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostRepository;
import com.dhoon.transfertracker.internal.transfer.service.TransferPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class TransferPostServiceImpl implements TransferPostService {

    private final TransferPostRepository transferPostRepository;
    private final TeamRepository teamRepository;


    @Override
    public TransferPostsResponseDto getSourcerAllPosts(TransferPostSource sourcer) {
        List<TransferPostItemResponseDto> posts = transferPostRepository.findAllBySourcer(sourcer)
                .stream()
                .map(TransferPostItemResponseDto::of)
                .toList();

        return TransferPostsResponseDto.of(posts);
    }


    // 특정 팀에 대한 게시물 분류 -> 외부 폴더에 있는게 더 좋지않을까 ? ( external .. )
    @Transactional
    @Override
    public String test() {
        List<TransferPost> posts = transferPostRepository.findAll();

        List<Team> eplTeamNames = teamRepository.findAllByLeagueCode(LeagueCode.EPL);

        for (TransferPost post : posts) {
            String lowerContent = post.getContent().toLowerCase();

            eplTeamNames.forEach(team -> {
                if (lowerContent.contains(team.getTeamName().toLowerCase())) {

                    post.assignTeam(team);
                }
            });
        }

        return "TEST";
    }


    @Override
    public TransferPostsResponseDto getTeamTransferPosts(Long teamId) {
        List<TransferPostItemResponseDto> posts = transferPostRepository.findAllByTeamId(teamId)
                .stream()
                .map(TransferPostItemResponseDto::of)
                .toList();

        return TransferPostsResponseDto.of(posts);
    }

    @Override
    public TransferPostsResponseDto getAllTransferPosts() {
        List<TransferPostItemResponseDto> posts = transferPostRepository.findAll()
                .stream()
                .map(TransferPostItemResponseDto::of)
                .toList();

        return TransferPostsResponseDto.of(posts);
    }

}
