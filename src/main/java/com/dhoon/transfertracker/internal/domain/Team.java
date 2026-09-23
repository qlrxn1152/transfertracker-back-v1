package com.dhoon.transfertracker.internal.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    private Team(String teamName, Long apiFootballId) {
        this.teamName = teamName;
        this.apiFootballId = apiFootballId;
    }

    public static Team of(String teamName, Long apiFootballId) {
        return new Team(teamName, apiFootballId);
    }

}
