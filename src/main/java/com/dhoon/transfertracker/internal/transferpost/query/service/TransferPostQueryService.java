package com.dhoon.transfertracker.internal.transferpost.query.service;

import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostDetailResponseDto;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostsResponseDto;

public interface TransferPostQueryService {

    TransferPostsResponseDto getSourcerAllPosts(TransferPostSource sourcer);

    TransferPostsResponseDto getTeamTransferPosts(Long teamId);

    TransferPostsResponseDto getAllTransferPosts();

    TransferPostDetailResponseDto getTransferPost(Long postId);


}
