package com.dhoon.transfertracker.internal.analytics.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PageViewCreateRequestDto {

    @NotBlank
    @Size(max = 80)
    private String visitorId;

    @NotBlank
    @Size(max = 80)
    private String sessionId;

    @NotBlank
    @Size(max = 500)
    private String path;

    @Size(max = 200)
    private String pageTitle;

    @Size(max = 500)
    private String referrer;
}
