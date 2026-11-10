package com.dhoon.transfertracker.internal.transferpost.query.service;

import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostDetailResponseDto;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostsPageResponseDto;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostsResponseDto;
import org.springframework.data.domain.Pageable;

public interface TransferPostQueryService {

    TransferPostsPageResponseDto getSourcerAllPosts(TransferPostSource sourcer, Pageable pageable);

    TransferPostsResponseDto getTeamTransferPosts(Long teamId, Pageable pageable);

    TransferPostsResponseDto getAllTransferPosts(Pageable pageable);

    TransferPostDetailResponseDto getTransferPost(Long postId);


}
