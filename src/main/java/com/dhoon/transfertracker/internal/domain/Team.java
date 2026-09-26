package com.dhoon.transfertracker.internal.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Team {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_id")
    private Long id;

    @Column(name = "team_name", nullable = false)
    private String teamName;

    @Column(name = "api_football_id", nullable = false, unique = true)
    private Long apiFootballId;

    @Enumerated(EnumType.STRING)
    @Column(name = "league_api_code")
    private LeagueCode leagueCode;




    public void assignTeamLeague(LeagueCode leagueCode) {
        this.leagueCode = leagueCode;
    }

    private Team(String teamName, Long apiFootballId) {
        this.teamName = teamName;
        this.apiFootballId = apiFootballId;
    }

    private Team(String teamName, Long apiFootballId, LeagueCode leagueCode) {
        this.teamName = teamName;
        this.apiFootballId = apiFootballId;
        this.leagueCode = leagueCode;
    }


    public static Team of(String teamName, Long apiFootballId) {
        return new Team(teamName, apiFootballId);
    }

    public static Team of(String teamName, Long apiFootballId,  LeagueCode leagueCode) {
        return new Team(teamName, apiFootballId, leagueCode);
    }


}
