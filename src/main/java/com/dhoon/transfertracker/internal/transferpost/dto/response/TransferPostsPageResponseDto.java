package com.dhoon.transfertracker.internal.transferpost.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TransferPostsPageResponseDto {

    private List<TransferPostItemResponseDto> posts = new ArrayList<>();
    private long totalPostCount;
    private int totalPageCount;

    public static TransferPostsPageResponseDto of(List<TransferPostItemResponseDto> posts, long totalPostCount, int totalPageCount) {
        return new TransferPostsPageResponseDto(posts, totalPostCount, totalPageCount);
    }

}
