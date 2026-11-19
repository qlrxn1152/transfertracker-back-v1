package com.dhoon.transfertracker.internal.post.domain;

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
        name = "post",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_post_external_post_id", columnNames = "external_post_id")
        },
        indexes = {
                @Index(name = "idx_post_related", columnList = "is_transfer_related")
        }
)
public class Post {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "journalist_name", nullable = false)
    private PostJournalist journalist;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "external_post_id", nullable = false, unique = true)
    private String externalPostId;

    @Column(name = "content_created_at", nullable = false)
    private LocalDateTime contentCreatedAt;

    @Column(name = "is_transfer_related", nullable = false)
    private boolean transferRelated;

    public static Post createPost(PostJournalist journalist, String content, String externalPostId, boolean transferRelated) {
        return new Post(journalist, content, externalPostId, transferRelated);
    }

    public void translate(String content) {
        this.content = content;
    }





    private Post(
            PostJournalist journalist,
            String content,
            String externalPostId,
            boolean transferRelated
    ) {
        this.journalist = journalist;
        this.content = content;
        this.externalPostId = externalPostId;
        this.transferRelated = transferRelated;
        this.contentCreatedAt = LocalDateTime.now();
    }

}
