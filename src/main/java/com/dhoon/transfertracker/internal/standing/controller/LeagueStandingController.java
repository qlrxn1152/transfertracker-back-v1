package com.dhoon.transfertracker.internal.standing.controller;

import com.dhoon.transfertracker.internal.standing.dto.record.LeagueStandingGroupResponse;
import com.dhoon.transfertracker.internal.standing.service.LeagueStandingQueryService;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class LeagueStandingController {

    private final LeagueStandingQueryService leagueStandingQueryService;

    @GetMapping("/api/standings")
    public List<LeagueStandingGroupResponse> getLeagueStandings(
            @RequestParam LeagueCode leagueCode,
            @RequestParam(required = false) Integer season
    ) {

        if (season == null) {
            season = leagueCode.resolveSeason(
                    LocalDate.now(ZoneId.of("Asia/Seoul"))
            );
        }

        return leagueStandingQueryService.getLeagueStandings(
                leagueCode,
                season
        );
    }
}