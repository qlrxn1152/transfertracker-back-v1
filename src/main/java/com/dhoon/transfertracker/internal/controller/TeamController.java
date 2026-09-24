package com.dhoon.transfertracker.internal.controller;

import com.dhoon.transfertracker.external.football.dto.team.TeamInfoResponseDto;
import com.dhoon.transfertracker.internal.dto.team.TeamItemResponseDto;
import com.dhoon.transfertracker.internal.dto.team.TeamsResponseDto;
import com.dhoon.transfertracker.internal.repository.TeamRepository;
import com.dhoon.transfertracker.internal.service.TeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @GetMapping("/api/team/{teamId}")
    public ResponseEntity<TeamItemResponseDto> getTeam(@PathVariable Long teamId) {
        TeamItemResponseDto response = teamService.getTeam(teamId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/teams")
    public ResponseEntity<TeamsResponseDto> getTeams() {
        TeamsResponseDto response = teamService.getTeams();

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }






}
