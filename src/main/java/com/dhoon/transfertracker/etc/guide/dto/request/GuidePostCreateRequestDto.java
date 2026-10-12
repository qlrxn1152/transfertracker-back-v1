package com.dhoon.transfertracker.etc.guide.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GuidePostCreateRequestDto {

    @NotBlank
    @Size(max = 160)
    private String title;

    @NotBlank
    @Size(max = 10000)
    private String content;

    @Size(max = 8)
    private List<String> imageUrls = new ArrayList<>();
}
