package com.dhoon.transfertracker.internal.transferpost.command.service;

import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPost;
import com.dhoon.transfertracker.internal.transferpost.repository.TransferPostRepository;
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
