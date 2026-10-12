package com.dhoon.transfertracker.etc.guide.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuidePost {

    private static final String IMAGE_SEPARATOR = "\n---TRANSFER_TRACKER_GUIDE_IMAGE---\n";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "guide_post_id")
    private Long id;

    @Column(nullable = false, length = 160)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String imageUrlsText;

    @Column(nullable = false, length = 80)
    private String authorLoginId;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private GuidePost(String title, String content, List<String> imageUrls, String authorLoginId) {
        this.title = title;
        this.content = content;
        this.imageUrlsText = joinImageUrls(imageUrls);
        this.authorLoginId = authorLoginId;
        this.active = true;
    }

    public static GuidePost of(String title, String content, List<String> imageUrls, String authorLoginId) {
        return new GuidePost(title, content, imageUrls, authorLoginId);
    }

    public void update(String title, String content, List<String> imageUrls) {
        this.title = title;
        this.content = content;
        this.imageUrlsText = joinImageUrls(imageUrls);
    }

    public void deactivate() {
        this.active = false;
    }

    public List<String> getImageUrls() {
        if (imageUrlsText == null || imageUrlsText.isBlank()) {
            return List.of();
        }

        return Arrays.stream(imageUrlsText.split(IMAGE_SEPARATOR))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    private String joinImageUrls(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return "";
        }

        return String.join(
                IMAGE_SEPARATOR,
                imageUrls.stream()
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .toList()
        );
    }
}
