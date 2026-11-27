package com.dhoon.transfertracker.internal.standing.service;

import com.dhoon.transfertracker.internal.standing.domain.LeagueStanding;
import com.dhoon.transfertracker.internal.standing.dto.record.ApiFootballStandingResponse;
import com.dhoon.transfertracker.internal.standing.repository.LeagueStandingRepository;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.domain.Team;
import com.dhoon.transfertracker.internal.team.repository.TeamRepository;
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
public class LeagueStandingSyncTxService {

    private final LeagueStandingRepository leagueStandingRepository;
    private final TeamRepository teamRepository;

    public void syncLeagueStandings(
            LeagueCode leagueCode,
            Integer season,
            List<ApiFootballStandingResponse> standings
    ) {

        if (standings == null || standings.isEmpty()) {
            return;
        }

        for (ApiFootballStandingResponse response : standings) {
            validateStanding(response);
        }

        List<Long> teamApiIds = standings.stream()
                .map(ApiFootballStandingResponse::teamApiId)
                .distinct()
                .toList();

        Map<Long, Team> teamMap = teamRepository
                .findAllByApiFootballIdIn(teamApiIds)
                .stream()
                .collect(Collectors.toMap(
                        Team::getApiFootballId,
                        Function.identity()
                ));

        Map<StandingKey, LeagueStanding> standingMap =
                leagueStandingRepository
                        .findAllByLeagueCodeAndSeason(
                                leagueCode,
                                season
                        )
                        .stream()
                        .collect(Collectors.toMap(
                                standing -> new StandingKey(
                                        standing.getGroupName(),
                                        standing.getTeam().getId()
                                ),
                                Function.identity()
                        ));

        List<LeagueStanding> newStandings = new ArrayList<>();

        for (ApiFootballStandingResponse response : standings) {

            Team team = teamMap.get(response.teamApiId());

            if (team == null) {
                throw new IllegalStateException(
                        "등록되지 않은 팀입니다. teamApiId="
                                + response.teamApiId()
                );
            }

            StandingKey key = new StandingKey(
                    response.groupName(),
                    team.getId()
            );

            LeagueStanding standing = standingMap.get(key);

            if (standing == null) {

                standing = LeagueStanding.createLeagueStanding(
                        team,
                        leagueCode,
                        season,
                        response.groupName()
                );

                newStandings.add(standing);
                standingMap.put(key, standing);
            }

            updateStanding(standing, response);
        }

        leagueStandingRepository.saveAll(newStandings);

        log.info(
                "리그 순위 동기화 완료. leagueCode={}, season={}, total={}, created={}",
                leagueCode,
                season,
                standings.size(),
                newStandings.size()
        );
    }

    private void updateStanding(
            LeagueStanding standing,
            ApiFootballStandingResponse response
    ) {

        standing.updateStanding(
                response.ranking(),
                response.points(),
                response.goalsDiff(),
                response.form(),
                response.status(),
                response.description(),
                response.sourceUpdatedAt()
        );

        ApiFootballStandingResponse.StandingRecordResponse all =
                response.all();

        standing.updateAllRecord(
                all.played(),
                all.win(),
                all.draw(),
                all.lose(),
                all.goalsFor(),
                all.goalsAgainst()
        );

        ApiFootballStandingResponse.StandingRecordResponse home =
                response.home();

        standing.updateHomeRecord(
                home.played(),
                home.win(),
                home.draw(),
                home.lose(),
                home.goalsFor(),
                home.goalsAgainst()
        );

        ApiFootballStandingResponse.StandingRecordResponse away =
                response.away();

        standing.updateAwayRecord(
                away.played(),
                away.win(),
                away.draw(),
                away.lose(),
                away.goalsFor(),
                away.goalsAgainst()
        );
    }

    private void validateStanding(
            ApiFootballStandingResponse response
    ) {
        if (response.teamApiId() == null
                || response.teamApiId() <= 0
                || response.ranking() == null
                || response.ranking() <= 0
                || response.points() == null
                || response.goalsDiff() == null
                || response.groupName() == null
                || response.groupName().isBlank()) {

            throw new IllegalArgumentException(
                    "필수 리그 순위 정보가 누락되었습니다."
            );
        }
    }

    private record StandingKey(
            String groupName,
            Long teamId
    ) {
    }
}