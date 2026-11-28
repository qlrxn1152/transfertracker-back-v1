package com.dhoon.transfertracker.internal.team.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_team_api_football_id", columnNames = "api_football_id"),
                @UniqueConstraint(name = "uk_team_name", columnNames = "team_name"),
                @UniqueConstraint(name = "uk_team_name_ko", columnNames = "team_name_ko"),
        }
)
public class Team {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_id")
    private Long id;

    @Column(name = "team_name", nullable = false, unique = true)
    private String teamName;

    @Column(name = "api_football_id", nullable = false, unique = true)
    private Long apiFootballId;

    @Enumerated(EnumType.STRING)
    @Column(name = "league_api_code")
    private LeagueCode leagueCode;

    @Column(name = "team_name_ko", unique = true)
    private String teamNameKo;

    @Column(name = "country")
    private String country;

    @Column(name = "national")
    private Boolean national;

    public void updateTeamMetadata(
            String country,
            Boolean national
    ) {
        if (country != null && !country.isBlank()) {
            this.country = country;
        }

        if (national != null) {
            this.national = national;
        }
    }




    public static Team of(String teamName, Long apiFootballId) {
        if (apiFootballId == null || apiFootballId <= 0) {
            throw new IllegalArgumentException("Team Api Football ID 의 값이 올바르지 않습니다. ID = " + apiFootballId);
        }

        if (teamName == null || teamName.isBlank() || teamName.strip().equalsIgnoreCase("null")) {
            throw new IllegalArgumentException("영문 TeamName 의 값이 올바르지 않습니다. teamName = " + teamName);
        }

        return new Team(
                getStrippedTeamName(teamName),
                apiFootballId
        );
    }

    public void assignKoTeamName(String teamNameKo) {
        if (teamNameKo == null || teamNameKo.isBlank() || teamNameKo.strip().equalsIgnoreCase("null")) {
            throw new IllegalArgumentException("한국어 TeamName 의 값이 올바르지 않습니다. teamNameKo = " +  teamNameKo);
        }

        this.teamNameKo = getStrippedTeamName(teamNameKo);
    }

    public void assignTeamLeague(LeagueCode leagueCode) {
        if (leagueCode == null) {
            throw new IllegalArgumentException("팀 리그코드의 값은 null 일 수 없습니다.");
        }

        this.leagueCode = leagueCode;
    }


    public String getDisplayName() {
        boolean hasKoreanTeamName = teamNameKo != null && !teamNameKo.isBlank();

        return hasKoreanTeamName ? teamNameKo : teamName;
    }



    // ================================================ //
    private Team(String teamName, Long apiFootballId) {
        this.teamName = teamName;
        this.apiFootballId = apiFootballId;
    }

    private static String getStrippedTeamName(String teamName) {
        return teamName.strip();
    }



}
