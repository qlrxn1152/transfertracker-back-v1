package com.dhoon.transfertracker.internal.team.sync.service.impl;

import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
import com.dhoon.transfertracker.internal.team.sync.dto.request.TeamCollectionImportItem;
import com.dhoon.transfertracker.internal.team.sync.dto.request.TeamCollectionImportRequest;
import com.dhoon.transfertracker.internal.team.sync.dto.response.TeamCollectionImportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamCollectionImportService {

    private final TeamRepository teamRepository;

    public TeamCollectionImportResponse importTeams(TeamCollectionImportRequest request) {
        int created = 0;
        int updated = 0;
        int skippedNational = 0;

        for (TeamCollectionImportItem item : request.teams()) {
            if (Boolean.TRUE.equals(item.national())) {
                skippedNational++;
                continue;
            }

            Optional<Team> existingByApiFootballId = teamRepository
                    .findByApiFootballId(item.apiFootballId());
            Optional<Team> existingByName = existingByApiFootballId.isEmpty()
                    // Historical data can have the same club stored under a prior provider ID.
                    // Preserve that record and attach the current collection classification.
                    ? teamRepository.findByTeamName(item.teamName().strip())
                    : Optional.empty();
            Team team = existingByApiFootballId
                    .or(() -> existingByName)
                    .orElseGet(() -> teamRepository.save(
                            Team.of(item.teamName(), item.apiFootballId())
                    ));

            if (existingByApiFootballId.isEmpty() && existingByName.isEmpty()) {
                created++;
            } else {
                updated++;
            }

            team.updateTeamMetadata(item.country(), item.national());
            team.assignCollectionLeague(item.collectionLeagueCode());
        }

        return new TeamCollectionImportResponse(
                request.teams().size(),
                created,
                updated,
                skippedNational
        );
    }
}
