package com.dhoon.transfertracker.internal.transferpost.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TransferPostSource {

    FABRIZIO_ROMANO("FabrizioRomano", "330262748", "파브리지오 로마노"),
    DAVID_ORNSTEIN("David_Ornstein", "46875124", "데이비드 온스테인"),
    MATTEO_MORETTO("MatteMoretto", "975909216824254464", "마테오 모레토"),
    FLORIAN_PLETTENBERG("Plettigoal", "2303765948", "플로리안 플레텐베르크"),
    SANTI_AOUNA("Santi_J_FM", "1199736038874324993", "산티 아우나"),
    DI_MARZIO("DiMarzio", "251690031", "디 마르지오");

    private final String xUsername;
    private final String xUserId;
    private final String displayerName;

}
