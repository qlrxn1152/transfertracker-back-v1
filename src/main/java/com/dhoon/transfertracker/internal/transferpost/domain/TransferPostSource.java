package com.dhoon.transfertracker.internal.transferpost.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TransferPostSource {
    FABRIZIO_ROMANO("FabrizioRomano", "330262748"),
    DAVID_ORNSTEIN("David_Ornstein", "46875124"),
    MATTEO_MORETTO("MatteMoretto", "975909216824254464");

    private final String xUsername;
    private final String xUserId;

}
