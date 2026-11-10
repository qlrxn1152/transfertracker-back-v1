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
public class TransferPostsResponseDto {

    private List<TransferPostItemResponseDto> posts = new ArrayList<>();

    private long totalPostCount;
    private int totalPageCount;

    private boolean hasNext;
    private boolean hasPrevious;

    public static TransferPostsResponseDto of(List<TransferPostItemResponseDto> posts, long totalPostCount, int totalPageCount, boolean hasNext, boolean hasPrevious) {
        return new TransferPostsResponseDto(posts, totalPostCount, totalPageCount, hasNext, hasPrevious);
    }
}
