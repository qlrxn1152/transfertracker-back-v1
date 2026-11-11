package com.dhoon.transfertracker.internal.member.domain;

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
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_member_favorite_team_member_team", columnNames = {"member_id", "team_id"})
        }
)
public class MemberFavoriteTeam {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_favorite_team_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static MemberFavoriteTeam of(Member member, Team team) {
        return new MemberFavoriteTeam(member, team);
    }


    public MemberFavoriteTeam(Member member, Team team) {
        this.member = member;
        this.team = team;
        this.createdAt = Instant.now();
    }

}
