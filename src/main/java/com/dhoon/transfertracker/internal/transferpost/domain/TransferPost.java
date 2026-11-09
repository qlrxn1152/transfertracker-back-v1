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
                @UniqueConstraint(name = "uk_transfer_post_sourcer_content", columnNames = {"sourcer_name", "content"}),
                @UniqueConstraint(name = "uk_transfer_post_external_post_id", columnNames = "external_post_id")
        },

        indexes = {
                @Index(name = "idx_transfer_post_related", columnList = "is_transfer_related"),
                @Index(name = "idx_transfer_post_status", columnList = "translate_status")
        }
)
public class TransferPost {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transfer_post_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "sourcer_name", nullable = false)
    private TransferPostSource sourcer;

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
    private TranslateStatus translateStatus;

    @Column(name = "translated_at")
    private Instant translatedAt;

    private TransferPost(TransferPostSource sourcer, String content, String externalPostId, Instant contentCreatedAt, boolean transferRelated, String photoUrl) {
        this.sourcer = sourcer;
        this.content = content;
        this.externalPostId = externalPostId;
        this.contentCreatedAt = contentCreatedAt;
        this.transferRelated = transferRelated;
        this.photoUrl = photoUrl;

        this.translatedContent = null;
        this.translateStatus = TranslateStatus.PENDING;
    }

    public static TransferPost of(TransferPostSource sourcer, String content, String externalPostId, Instant contentCreatedAt, boolean transferRelated) {
        return new TransferPost(sourcer, content, externalPostId, contentCreatedAt, transferRelated, null);
    }

    public static TransferPost of(TransferPostSource sourcer, String content, String externalPostId, Instant contentCreatedAt, boolean transferRelated, String photoUrl) {
        return new TransferPost(sourcer, content, externalPostId, contentCreatedAt, transferRelated, photoUrl);
    }

    public void translate(String translatedContent) {
        this.translatedContent = translatedContent;
        this.translateStatus = TranslateStatus.COMPLETED;
        this.translatedAt = Instant.now();
    }

    public void failTranslate() {
        this.translateStatus = TranslateStatus.FAILED;
    }


}
