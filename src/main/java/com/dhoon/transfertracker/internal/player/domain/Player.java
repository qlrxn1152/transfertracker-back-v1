package com.dhoon.transfertracker.internal.player.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        indexes =
            @Index(name = "idx_player_player_name", columnList = "player_name")
)
public class Player {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "player_id")
    private Long id;

    @Column(name = "player_name", nullable = false)
    private String playerName;

    @Column(name = "api_football_id", nullable = false, unique = true)
    private Long apiFootballId;

    @Column(name = "player_name_ko")
    private String playerNameKo;

    private Player(String playerName, Long apiFootballId) {
        this.playerName = playerName;
        this.apiFootballId = apiFootballId;
    }




    /**
     * 외부 API에서 수집한 선수 정보를 기반으로 Player 엔티티를 생성합니다.
     *
     * <p>한국어 이름은 생성 시 설정하지 않으며,
     * 이후 별도의 번역 작업을 통해 등록합니다.</p>
     *
     * @param playerName 선수의 영문 이름
     * @param apiFootballId API-Football에서 제공하는 선수 식별자
     * @return 생성된 Player 엔티티
     */
    public static Player of(String playerName, Long apiFootballId) {

        if (playerName == null || playerName.isBlank()) {
            throw new IllegalArgumentException("선수의 이름은 비어 있을 수 없습니다.");
        }

        if (apiFootballId == null || apiFootballId <= 0) {
            throw new IllegalArgumentException("선수의 API ID 의 값은 자연수이어야합니다.");
        }

        String normalizedPlayerName = normalizePlayerName(playerName);

        return new Player(normalizedPlayerName, apiFootballId);
    }




    /**
     * 선수의 한국어 이름을 등록하거나 변경한다.
     *
     * @param playerNameKo 선수의 한국어 이름
     */
    public void assignKoPlayerName(String playerNameKo) {

        if (playerNameKo == null || playerNameKo.isBlank()) {
            throw new IllegalArgumentException("선수의 한국어 이름은 비어 있을 수 없습니다.");
        }

        String normalizedPlayerNameKo = normalizePlayerName(playerNameKo);


        this.playerNameKo = normalizedPlayerNameKo;
    }


    /**
     * 화면에 표시할(사용자에게 보여질) 선수 이름을 반환한다.
     *
     * <p>한국어 이름이 존재하면 한국어 이름을 우선 반환하고,
     * 존재하지 않거나 공백이면 영문 이름을 반환한다.</p>
     *
     * @return 화면에 표시할(사용자에게 보여질) 선수 이름
     */
    public String getDisplayName() {

        boolean hasKoreanName = (playerNameKo != null && !playerNameKo.isBlank());

        return hasKoreanName ? playerNameKo : playerName;
    }

    private static String normalizePlayerName(String playerName) {
        return playerName.strip();
    }

}
