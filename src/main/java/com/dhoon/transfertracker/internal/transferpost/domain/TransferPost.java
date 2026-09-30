package com.dhoon.transfertracker.internal.transferpost.domain;

import com.dhoon.transfertracker.internal.team.domain.Team;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_transfer_post",
                        columnNames = {"sourcer_name", "content"}
                )
        }
)
public class TransferPost {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transfer_post_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "sourcer_name")
    private TransferPostSource sourcer;

    @Column(name = "content", nullable = false, length = 500)
    private String content;

    @Column(name = "external_post_id", nullable = false, unique = true)
    private String externalPostId;

    @Column(name = "contentCreatedAt")
    private Instant contentCreatedAt;

    @Column(name = "is_transfer_related")
    private boolean transferRelated;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    private TransferPost(TransferPostSource sourcer, String content, String externalPostId, Instant contentCreatedAt, boolean transferRelated) {
        this.sourcer = sourcer;
        this.content = content;
        this.externalPostId = externalPostId;
        this.contentCreatedAt = contentCreatedAt;
        this.transferRelated = transferRelated;
    }

    public static TransferPost of(TransferPostSource sourcer, String content, String externalPostId, Instant contentCreatedAt, boolean transferRelated) {
        return new TransferPost(sourcer, content, externalPostId, contentCreatedAt, transferRelated);
    }

    public void assignTeam(Team team) {
        this.team = team;
    }


}
