package com.dhoon.transfertracker.internal.team.controller;

import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
import com.dhoon.transfertracker.internal.team.dto.response.TeamItemResponseDto;
import com.dhoon.transfertracker.internal.team.dto.response.TeamPageInfosResponseDto;
import com.dhoon.transfertracker.internal.team.dto.response.TeamsResponseDto;
import com.dhoon.transfertracker.internal.team.service.impl.TeamOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class TeamController {

    private final TeamOrchestrationService teamOrchestrationService;

    @GetMapping("/api/team/{teamId}")
    public ResponseEntity<TeamItemResponseDto> getTeam(@PathVariable Long teamId) {
        TeamItemResponseDto response = teamOrchestrationService.getTeam(teamId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/teams")
    public ResponseEntity<TeamsResponseDto> getTeams() {
        TeamsResponseDto response = teamOrchestrationService.getTeams();

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/api/teams/league")
    public ResponseEntity<TeamsResponseDto> getLeagueTeams(@RequestParam LeagueCode leagueCode) {
        TeamsResponseDto response = teamOrchestrationService.getLeagueTeams(leagueCode);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/api/team/test/{teamId}")
    public ResponseEntity<TeamPageInfosResponseDto> getTeamInfo(@PathVariable Long teamId) {
        TeamPageInfosResponseDto response = teamOrchestrationService.getTeamInfo(teamId);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

}
