package com.dhoon.transfertracker.external.openai.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TranslationResult {

    private Long postId;
    private String translatedContent;

    public static TranslationResult of(Long postId, String translatedContent) {
        return new TranslationResult(postId, translatedContent);
    }
}
