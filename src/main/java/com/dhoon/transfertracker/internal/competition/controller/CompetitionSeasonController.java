package com.dhoon.transfertracker.internal.competition.controller;

import com.dhoon.transfertracker.internal.competition.dto.record.CompetitionSeasonResponse;
import com.dhoon.transfertracker.internal.competition.service.CompetitionSeasonQueryService;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/competitions")
public class CompetitionSeasonController {

    private final CompetitionSeasonQueryService competitionSeasonQueryService;

    @GetMapping
    public List<CompetitionSeasonResponse> getCurrentSeasons() {

        return competitionSeasonQueryService.getCurrentSeasons();
    }

    @GetMapping("/{leagueCode}/seasons")
    public List<CompetitionSeasonResponse> getCompetitionSeasons(
            @PathVariable LeagueCode leagueCode
    ) {

        return competitionSeasonQueryService.getCompetitionSeasons(
                leagueCode
        );
    }
}