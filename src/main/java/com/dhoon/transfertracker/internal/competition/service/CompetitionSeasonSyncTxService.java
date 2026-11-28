package com.dhoon.transfertracker.internal.competition.service;

import com.dhoon.transfertracker.internal.competition.domain.CompetitionSeason;
import com.dhoon.transfertracker.internal.competition.dto.record.ApiFootballCompetitionSeasonResponse;
import com.dhoon.transfertracker.internal.competition.repository.CompetitionSeasonRepository;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CompetitionSeasonSyncTxService {

    private final CompetitionSeasonRepository competitionSeasonRepository;

    public void syncCompetitionSeasons(
            LeagueCode leagueCode,
            List<ApiFootballCompetitionSeasonResponse> seasons
    ) {

        if (seasons == null || seasons.isEmpty()) {
            return;
        }

        Map<Integer, CompetitionSeason> seasonMap =
                competitionSeasonRepository
                        .findAllByLeagueCode(leagueCode)
                        .stream()
                        .collect(Collectors.toMap(
                                CompetitionSeason::getSeason,
                                Function.identity()
                        ));

        List<CompetitionSeason> newSeasons = new ArrayList<>();

        for (ApiFootballCompetitionSeasonResponse response : seasons) {

            CompetitionSeason competitionSeason =
                    seasonMap.get(response.season());

            if (competitionSeason == null) {

                competitionSeason =
                        CompetitionSeason.createCompetitionSeason(
                                leagueCode,
                                response.season()
                        );

                newSeasons.add(competitionSeason);

                seasonMap.put(
                        response.season(),
                        competitionSeason
                );
            }

            competitionSeason.updateSeason(
                    response.startDate(),
                    response.endDate(),
                    response.currentSeason()
            );

            competitionSeason.updateCoverage(
                    response.supportsEvents(),
                    response.supportsLineups(),
                    response.supportsFixtureStatistics(),
                    response.supportsPlayerStatistics(),
                    response.supportsStandings(),
                    response.supportsPlayers(),
                    response.supportsTopScorers(),
                    response.supportsTopAssists(),
                    response.supportsTopCards(),
                    response.supportsInjuries()
            );
        }

        competitionSeasonRepository.saveAll(newSeasons);

        log.info(
                "대회 시즌 동기화 완료. leagueCode={}, total={}, created={}",
                leagueCode,
                seasons.size(),
                newSeasons.size()
        );
    }
}