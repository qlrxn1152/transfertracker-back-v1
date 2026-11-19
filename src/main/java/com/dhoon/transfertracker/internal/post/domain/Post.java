package com.dhoon.transfertracker.internal.post.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

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

    @Column(name = "content", nullable = false, length = 500)
    private String content;

    @Column(name = "external_post_id", nullable = false, unique = true)
    private String externalPostId;

    @Column(name = "content_created_at", nullable = false)
    private Instant contentCreatedAt;

    @Column(name = "is_transfer_related", nullable = false)
    private boolean transferRelated;

    @Column(name = "photo_url", length = 1000)
    private String photoUrl;

    @Column(name = "translated_content", columnDefinition = "TEXT")
    private String translatedContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "translate_status", nullable = false)
    private PostTranslateStatus translateStatus;

    @Column(name = "translated_at")
    private Instant translatedAt;

    public static Post createPost(
            PostJournalist journalist,
            String content,
            String externalPostId,
            Instant contentCreatedAt,
            boolean transferRelated,
            String photoUrl
    ) {
        return new Post(journalist, content, externalPostId, contentCreatedAt, transferRelated, photoUrl);
    }

    public void translate(String translatedContent) {
        this.translatedContent = translatedContent;
        this.translateStatus = PostTranslateStatus.COMPLETED;
        this.translatedAt = Instant.now();
    }

    public String getDisplayContent() {
        return translatedContent == null || translatedContent.isBlank() ? content : translatedContent;
    }





    private Post(
            PostJournalist journalist,
            String content,
            String externalPostId,
            Instant contentCreatedAt,
            boolean transferRelated,
            String photoUrl
    ) {
        this.journalist = journalist;
        this.content = content;
        this.externalPostId = externalPostId;
        this.contentCreatedAt = contentCreatedAt;
        this.transferRelated = transferRelated;
        this.photoUrl = photoUrl;
        this.translateStatus = PostTranslateStatus.PENDING;
    }
}
