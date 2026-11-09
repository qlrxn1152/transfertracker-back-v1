package com.dhoon.transfertracker.internal.transferpost.domain;

import com.dhoon.transfertracker.internal.team.domain.Team;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "transfer_post_team",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_transfer_post_team", columnNames = {"team_id", "post_id"})
        },
        indexes = {
                @Index(name = "idx_transfer_post_team_post", columnList = "post_id")
        }
)
public class TransferPostTeam {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transfer_post_team_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private TransferPost post;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static TransferPostTeam postAssignTeam(Team team, TransferPost post) {
        return new TransferPostTeam(team, post);
    }









    private TransferPostTeam(Team team, TransferPost post) {
        this.team = team;
        this.post = post;
        this.createdAt = Instant.now();
    }



}
