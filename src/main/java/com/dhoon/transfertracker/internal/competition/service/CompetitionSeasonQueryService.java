package com.dhoon.transfertracker.internal.competition.service;

import com.dhoon.transfertracker.internal.competition.domain.CompetitionSeason;
import com.dhoon.transfertracker.internal.competition.dto.record.CompetitionSeasonResponse;
import com.dhoon.transfertracker.internal.competition.repository.CompetitionSeasonRepository;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompetitionSeasonQueryService {

    private final CompetitionSeasonRepository competitionSeasonRepository;

    public List<CompetitionSeasonResponse> getCurrentSeasons() {

        return competitionSeasonRepository
                .findAllByCurrentSeasonTrue()
                .stream()
                .sorted(Comparator.comparingInt(
                        season -> season.getLeagueCode().ordinal()
                ))
                .map(CompetitionSeasonResponse::from)
                .toList();
    }

    public List<CompetitionSeasonResponse> getCompetitionSeasons(
            LeagueCode leagueCode
    ) {

        return competitionSeasonRepository
                .findAllByLeagueCodeOrderBySeasonDesc(leagueCode)
                .stream()
                .map(CompetitionSeasonResponse::from)
                .toList();
    }

    public int resolveCurrentSeason(LeagueCode leagueCode) {

        return competitionSeasonRepository
                .findFirstByLeagueCodeAndCurrentSeasonTrueOrderBySeasonDesc(
                        leagueCode
                )
                .map(CompetitionSeason::getSeason)
                .orElseThrow(() -> new IllegalStateException(
                        "현재 대회 시즌 정보가 없습니다. leagueCode="
                                + leagueCode
                ));
    }

    public List<CompetitionSeasonResponse> getStandingSupportedSeasons() {

        return competitionSeasonRepository
                .findAllByCurrentSeasonTrue()
                .stream()
                .filter(CompetitionSeason::isSupportsStandings)
                .map(CompetitionSeasonResponse::from)
                .toList();
    }
}