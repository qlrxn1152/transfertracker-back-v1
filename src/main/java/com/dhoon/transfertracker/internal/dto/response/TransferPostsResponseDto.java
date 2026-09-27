package com.dhoon.transfertracker.internal.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TransferPostsResponseDto {

    private List<TransferPostItemResponseDto> posts = new ArrayList<>();

    public static TransferPostsResponseDto of(List<TransferPostItemResponseDto> posts) {
        return new TransferPostsResponseDto(posts);
    }
}
