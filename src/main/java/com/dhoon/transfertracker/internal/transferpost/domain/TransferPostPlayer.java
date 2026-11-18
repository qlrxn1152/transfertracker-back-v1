package com.dhoon.transfertracker.internal.transferpost.domain;

import com.dhoon.transfertracker.internal.player.domain.Player;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_transfer_post_player_player_post", columnNames = {"player_id", "transfer_post_id"})
        },

        indexes = {
                @Index(name = "idx_transfer_post_player_post", columnList = "transfer_post_id")
        }
)
public class TransferPostPlayer {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transfer_post_player_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_post_id", nullable = false)
    private TransferPost transferPost;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static TransferPostPlayer postAssignPlayer(Player player, TransferPost transferPost) {
        return new TransferPostPlayer(player, transferPost);
    }

    private TransferPostPlayer(Player player, TransferPost transferPost) {
        this.player = player;
        this.transferPost = transferPost;
        this.createdAt = Instant.now();
    }
}
