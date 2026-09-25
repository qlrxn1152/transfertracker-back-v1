package com.dhoon.transfertracker.internal.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transfer {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transfer_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "in_team_id", nullable = false)
    private Team inTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "out_team_id", nullable = false)
    private Team outTeam;

    @Column(name = "transfer_type", nullable = false)
    private String transferType; // 이후 ENUM 교체 필요.

    @Column(name = "transfer_date", nullable = false)
    private LocalDate transferDate;

    private Transfer(Player player, Team inTeam, Team outTeam, String transferType, LocalDate transferDate) {
        this.player = player;
        this.inTeam = inTeam;
        this.outTeam = outTeam;
        this.transferType = transferType;
        this.transferDate = transferDate;
    }

    public static Transfer of(Player player, Team inTeam, Team outTeam, String transferType, LocalDate transferDate) {
        return new Transfer(player, inTeam, outTeam, transferType, transferDate);
    }


}
