package com.dhoon.transfertracker.internal.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TransferPostSource {
    FABRIZIO_ROMANO("FABRIZIO_ROMANO", "330262748"),
    DAVID_ORNSTEIN("DAVID_ORNSTEIN", "46875124");


    private final String xUsername;
    private final String xUserId;

}
