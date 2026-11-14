package com.dhoon.transfertracker.internal.transferpost.command.service.impl;

import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.transferpost.command.dto.TeamAssignPostResult;
import com.dhoon.transfertracker.internal.transferpost.command.dto.TeamAssignPostTarget;
import com.dhoon.transfertracker.internal.transferpost.command.dto.TeamAssignPostsResult;
import com.dhoon.transfertracker.internal.transferpost.command.dto.TeamAssignPostsTarget;
import com.dhoon.transfertracker.internal.transferpost.command.event.TransferPostTeamAssignedEvent;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostTeam;
import com.dhoon.transfertracker.internal.transferpost.exception.NotFoundTransferPostException;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostRepository;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostTeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
@Transactional
@Service
public class TransferPostCommandTxService {

    private final TransferPostRepository transferPostRepository;
    private final TeamRepository teamRepository;
    private final TransferPostTeamRepository transferPostTeamRepository;

    private final ApplicationEventPublisher eventPublisher;

    public TeamAssignPostsTarget findTargetTeams() {
        List<TeamAssignPostTarget> posts = transferPostRepository.findAll()
                .stream()
                .filter(post -> !transferPostTeamRepository.existsByPostId(post.getId()))
                .map(post -> new TeamAssignPostTarget(post.getId(), post.getContent()))
                .toList();

        return new TeamAssignPostsTarget(posts);
    }


    public String postAssignTeam(TeamAssignPostsResult posts) {

        List<TeamAssignPostResult> results = posts.posts();

        for (TeamAssignPostResult result : results) {

            Long postId = result.postId();

            TransferPost post = transferPostRepository.findById(postId)
                    .orElseThrow(NotFoundTransferPostException::new);

            List<String> assignedTeamNames = result.teamNames();

            assignedTeamNames.forEach(teamName -> {

                Optional<Team> optTeam = teamRepository.findByTeamName(teamName);

                if (optTeam.isEmpty()) {
                    log.warn(
                            "해당 팀명을 가진 팀이 존재하지 않습니다. postId={}, teamName={}",
                            postId,
                            teamName
                    );
                    return;
                }

                Team team = optTeam.get();

                TransferPostTeam transferPostTeam = transferPostTeamRepository.findByTeamIdAndPostId(team.getId(), postId)
                        .orElseGet(() -> {
                            TransferPostTeam saved = transferPostTeamRepository.save(TransferPostTeam.postAssignTeam(team, post));
                            eventPublisher.publishEvent(
                                    new TransferPostTeamAssignedEvent(
                                            postId,
                                            team.getId()
                                    )
                            );

                            return saved;
                        });
            });

        }

        return "OK";
    }




}
