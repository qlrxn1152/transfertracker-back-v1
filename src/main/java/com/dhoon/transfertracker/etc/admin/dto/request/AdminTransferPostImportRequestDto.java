package com.dhoon.transfertracker.etc.admin.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class AdminTransferPostImportRequestDto {

    @Valid
    @NotEmpty
    private List<AdminTransferPostImportItemRequestDto> posts;
}
