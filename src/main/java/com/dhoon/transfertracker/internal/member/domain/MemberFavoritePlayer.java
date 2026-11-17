package com.dhoon.transfertracker.internal.member.domain;

import com.dhoon.transfertracker.internal.player.domain.Player;
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
                @UniqueConstraint(name = "uk_member_favorite_player_member_player", columnNames = {"member_id", "player_id"})
        }
)
public class MemberFavoritePlayer {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_favorite_player_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static MemberFavoritePlayer of(Member member, Player player) {
        return new MemberFavoritePlayer(member, player);
    }

    private MemberFavoritePlayer(Member member, Player player) {
        this.member = member;
        this.player = player;
        this.createdAt = Instant.now();
    }
}
