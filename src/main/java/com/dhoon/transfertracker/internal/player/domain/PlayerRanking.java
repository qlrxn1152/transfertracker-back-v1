package com.dhoon.transfertracker.internal.player.domain;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import com.dhoon.transfertracker.internal.team.domain.LeagueCode;
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "player_ranking", uniqueConstraints = @UniqueConstraint(name = "uk_player_ranking_key", columnNames = {"player_id", "league_code", "season", "ranking_type"}))
public class PlayerRanking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "player_ranking_id")
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;
    @Enumerated(EnumType.STRING)
    @Column(name = "league_code", nullable = false)
    private LeagueCode leagueCode;
    @Column(name = "season", nullable = false)
    private Integer season;
    @Enumerated(EnumType.STRING)
    @Column(name = "ranking_type", nullable = false)
    private PlayerRankingType rankingType;
    @Column(name = "ranking")
    private Integer ranking;
    @Column(name = "stat_value")
    private Integer value;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static PlayerRanking of(Player player, LeagueCode leagueCode, Integer season, PlayerRankingType rankingType) {
        return new PlayerRanking(player, leagueCode, season, rankingType);
    }
    private PlayerRanking(Player player, LeagueCode leagueCode, Integer season, PlayerRankingType rankingType) {
        this.player = player;
        this.leagueCode = leagueCode;
        this.season = season;
        this.rankingType = rankingType;
    }
    public void update(
            Integer ranking,
            Integer value
    ) {
        this.ranking = ranking;
        this.value = value;
        this.updatedAt = Instant.now();
    }
}
