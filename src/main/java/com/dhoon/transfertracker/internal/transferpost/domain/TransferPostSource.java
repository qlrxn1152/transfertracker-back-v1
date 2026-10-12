package com.dhoon.transfertracker.internal.transferpost.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TransferPostSource {
    FABRIZIO_ROMANO("FabrizioRomano", "330262748"),
    DAVID_ORNSTEIN("David_Ornstein", "46875124"),
    MATTEO_MORETTO("MatteMoretto", "975909216824254464"),
    FLORIAN_PLETTENBERG("Plettigoal", "2303765948"),
    SANTI_AOUNA("Santi_J_FM", "1199736038874324993"),
    DI_MARZIO("DiMarzio", "251690031");

    private final String xUsername;
    private final String xUserId;

}
