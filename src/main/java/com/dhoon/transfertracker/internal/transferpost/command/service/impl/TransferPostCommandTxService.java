package com.dhoon.transfertracker.internal.transferpost.command.service.impl;

import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostTeam;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostRepository;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostTeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Transactional
@Service
public class TransferPostCommandTxService {

    private final TransferPostRepository transferPostRepository;
    private final TeamRepository teamRepository;
    private final TransferPostTeamRepository transferPostTeamRepository;


    public String postAssignTeam() {

        List<TransferPost> teamNotAssignPosts = transferPostRepository.findAll().stream()
                .filter(post -> !transferPostTeamRepository.existsByPostId(post.getId()))
                .toList();

        List<Team> allLeagueTeams = teamRepository.findAllByLeagueCodeIsNotNull();

        teamNotAssignPosts.forEach(post -> {
            String lowerContent = post.getContent().toLowerCase();

            allLeagueTeams.forEach(team -> {
                String lowerTeamName = team.getTeamName().toLowerCase();

                if (lowerContent.contains(lowerTeamName)) {
                    transferPostTeamRepository.save(TransferPostTeam.postAssignTeam(team, post));
                }

            });
        });

        return "OK";
    }


}
