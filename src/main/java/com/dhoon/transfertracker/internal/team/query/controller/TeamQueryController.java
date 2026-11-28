package com.dhoon.transfertracker.internal.team.query.controller;

import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.query.dto.response.record.TeamDetailResponseDto;
import com.dhoon.transfertracker.internal.team.query.dto.response.record.TeamItemResponseDto;
import com.dhoon.transfertracker.internal.team.query.dto.response.record.TeamsResponseDto;
import com.dhoon.transfertracker.internal.team.query.service.impl.TeamQueryOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/teams")
public class TeamQueryController {

    private final TeamQueryOrchestrationService teamQueryOrchestrationService;

    @GetMapping("/{teamId}")
    public ResponseEntity<TeamItemResponseDto> getTeam(@PathVariable Long teamId) {
        TeamItemResponseDto response = teamQueryOrchestrationService.getTeam(teamId);

        return ResponseEntity.ok(response);
    }

    @GetMapping()
    public ResponseEntity<TeamsResponseDto> getTeams() {
        TeamsResponseDto response = teamQueryOrchestrationService.getTeams();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/league")
    public ResponseEntity<TeamsResponseDto> getLeagueTeams(@RequestParam LeagueCode leagueCode) {
        TeamsResponseDto response = teamQueryOrchestrationService.getLeagueTeams(leagueCode);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{teamId}/detail")
    public ResponseEntity<TeamDetailResponseDto> getTeamInfo(@PathVariable Long teamId, @RequestParam(defaultValue = "0") int page) {
        TeamDetailResponseDto response = teamQueryOrchestrationService.getTeamInfo(teamId, page);

        return ResponseEntity.ok(response);
    }

}
