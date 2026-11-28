package com.dhoon.transfertracker.internal.team.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_team_api_football_id", columnNames = "api_football_id")
        }
)
public class Team {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_id")
    private Long id;

    @Column(name = "team_name", nullable = false)
    private String teamName;

    @Column(name = "api_football_id", nullable = false)
    private Long apiFootballId;

    @Enumerated(EnumType.STRING)
    @Column(name = "league_api_code")
    private LeagueCode leagueCode;

    @Column(name = "team_name_ko")
    private String teamNameKo;

    @Column(name = "country")
    private String country;

    @Column(name = "national")
    private Boolean national;


    public void updateTeamMetadata(String country, Boolean national) {
        if (country != null && !country.isBlank()) {
            this.country = country;
        }

        if (national != null) {
            this.national = national;
        }
    }

    public static Team createTeam(String teamName, Long apiFootballId) {
        if (apiFootballId == null || apiFootballId <= 0) {
            throw new IllegalArgumentException("Team Api Football ID 의 값이 올바르지 않습니다. ID = " + apiFootballId);
        }

        String strippedTeamName = validateAndNormalizeTeamName(teamName);

        return new Team(strippedTeamName, apiFootballId);
    }

    public void assignKoTeamName(String teamNameKo) {
        this.teamNameKo = validateAndNormalizeTeamName(teamNameKo);
    }

    public void assignTeamLeague(LeagueCode leagueCode) {

        if (leagueCode == null) {
            throw new IllegalArgumentException("팀 리그코드의 값은 null 일 수 없습니다.");
        }

        if (!leagueCode.isDomesticLeague()) {
            throw new IllegalArgumentException("팀에 할당할 수 없는 리그입니다.");
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

    private static String validateAndNormalizeTeamName(String teamName) {

        if (teamName == null) {
            throw new IllegalArgumentException("팀 이름은 null 일 수 없습니다.");
        }

        String normalizedTeamName = teamName.strip();

        if (normalizedTeamName.isEmpty() || normalizedTeamName.equalsIgnoreCase("null")) {
            throw new IllegalArgumentException("팀 이름의 값이 올바르지 않습니다. teamName = " + teamName);
        }

        return normalizedTeamName;
    }

}
