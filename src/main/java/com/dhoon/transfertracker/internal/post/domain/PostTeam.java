package com.dhoon.transfertracker.internal.post.domain;

import com.dhoon.transfertracker.internal.team.domain.Team;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "post_team",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_post_team_team_post", columnNames = {"team_id", "post_id"})
        },
        indexes = {
                @Index(name = "idx_post_team_post", columnList = "post_id")
        }
)
public class PostTeam {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_team_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static PostTeam postAssignTeam(Team team, Post post) {
        return new PostTeam(team, post);
    }

    private PostTeam(Team team, Post post) {
        this.team = team;
        this.post = post;
        this.createdAt = LocalDateTime.now();
    }
}
