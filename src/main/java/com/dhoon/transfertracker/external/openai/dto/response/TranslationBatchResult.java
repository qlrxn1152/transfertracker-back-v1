package com.dhoon.transfertracker.external.openai.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;


@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TranslationBatchResult {
    List<TranslationResult> posts = new ArrayList<>();

    public static TranslationBatchResult of(List<TranslationResult> posts) {
        return new TranslationBatchResult(posts);
    }
}
