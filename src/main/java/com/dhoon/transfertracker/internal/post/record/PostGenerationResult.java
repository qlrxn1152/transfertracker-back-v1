package com.dhoon.transfertracker.internal.post.record;

import java.math.BigDecimal;

public record PostGenerationResult(
        String externalPostId,
        Boolean transferRelated,
        String generationContent,
        BigDecimal transferFee,
        String transferFeeCurrency,
        Integer contractYears
) {
}
