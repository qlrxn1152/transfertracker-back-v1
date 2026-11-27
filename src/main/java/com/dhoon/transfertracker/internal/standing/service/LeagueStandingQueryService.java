package com.dhoon.transfertracker.internal.standing.service;

import com.dhoon.transfertracker.internal.standing.domain.LeagueStanding;
import com.dhoon.transfertracker.internal.standing.dto.record.LeagueStandingGroupResponse;
import com.dhoon.transfertracker.internal.standing.dto.record.LeagueStandingResponse;
import com.dhoon.transfertracker.internal.standing.repository.LeagueStandingRepository;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeagueStandingQueryService {

    private final LeagueStandingRepository leagueStandingRepository;

    public List<LeagueStandingGroupResponse> getLeagueStandings(
            LeagueCode leagueCode,
            Integer season
    ) {

        List<LeagueStanding> standings =
                leagueStandingRepository
                        .findAllByLeagueCodeAndSeason(
                                leagueCode,
                                season
                        );

        Map<String, List<LeagueStanding>> standingsByGroup =
                standings.stream()
                        .collect(Collectors.groupingBy(
                                LeagueStanding::getGroupName,
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

        return standingsByGroup.entrySet()
                .stream()
                .map(entry -> new LeagueStandingGroupResponse(
                        entry.getKey(),
                        entry.getValue()
                                .stream()
                                .map(LeagueStandingResponse::from)
                                .toList()
                ))
                .sorted(Comparator.comparingInt(
                        group -> groupOrder(group.groupName())
                ))
                .toList();
    }

    private int groupOrder(String groupName) {

        return switch (groupName) {
            case "K-League: Regular season" -> 0;
            case "K-League: Championship Group" -> 1;
            case "K-League: Relegation Group" -> 2;
            default -> 0;
        };
    }
}