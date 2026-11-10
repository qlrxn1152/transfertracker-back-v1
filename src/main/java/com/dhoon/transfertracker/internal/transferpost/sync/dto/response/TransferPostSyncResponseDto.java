package com.dhoon.transfertracker.internal.transferpost.sync.dto.response;

import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostItemResponseDto;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostsResponseDto;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TransferPostSyncResponseDto {

    private List<TransferPostItemResponseDto> posts = new ArrayList<>();

    public static TransferPostSyncResponseDto of(List<TransferPostItemResponseDto> posts) {
        return new TransferPostSyncResponseDto(posts);
    }

}
