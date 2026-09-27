package com.dhoon.transfertracker.internal.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TransferPostSource {
    FABRIZIO_ROMANO("FabrizioRomano", "330262748"),
    DAVID_ORNSTEIN("David_Ornstein", "46875124");


    private final String xUsername;
    private final String xUserId;

}
