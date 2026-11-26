package com.dhoon.transfertracker.internal.post.domain;

import com.dhoon.transfertracker.internal.player.domain.Player;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "post_player",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_post_player_player_post", columnNames = {"player_id", "post_id"})
        },
        indexes = {
                @Index(name = "idx_post_player_post", columnList = "post_id"),
                @Index(name = "idx_post_player_player_created_post", columnList = "player_id, post_created_at DESC, post_id DESC")

        }
)
public class PostPlayer {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_player_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "post_created_at", nullable = false, updatable = false)
    private LocalDateTime postCreatedAt;



    public static PostPlayer postAssignPlayer(Player player, Post post) {
        return new PostPlayer(player, post);
    }



    private PostPlayer(Player player, Post post) {
        this.player = player;
        this.post = post;
        this.createdAt = LocalDateTime.now();
        this.postCreatedAt = post.getContentCreatedAt();
    }
}
