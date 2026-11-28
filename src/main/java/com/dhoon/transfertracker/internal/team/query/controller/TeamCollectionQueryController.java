package com.dhoon.transfertracker.internal.team.query.controller;

import com.dhoon.transfertracker.internal.team.domain.TeamCollectionLeague;
import com.dhoon.transfertracker.internal.team.query.dto.response.TeamsResponseDto;
import com.dhoon.transfertracker.internal.team.query.service.impl.TeamQueryOrchestrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 수집 대상 정규리그 팀 조회. 컵·대항전은 팀 고정 소속으로 다루지 않는다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teams/collection")
public class TeamCollectionQueryController {

    private final TeamQueryOrchestrationService teamQueryOrchestrationService;

    @GetMapping("/league")
    public ResponseEntity<TeamsResponseDto> getLeagueTeams(
            @RequestParam TeamCollectionLeague leagueCode
    ) {
        return ResponseEntity.ok(
                teamQueryOrchestrationService.getCollectionLeagueTeams(leagueCode)
        );
    }
}
