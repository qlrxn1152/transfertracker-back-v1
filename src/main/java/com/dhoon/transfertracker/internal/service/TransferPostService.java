package com.dhoon.transfertracker.internal.service;

import com.dhoon.transfertracker.internal.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.dto.response.TransferPostItemResponseDto;
import com.dhoon.transfertracker.internal.dto.response.TransferPostsResponseDto;

public interface TransferPostService {

    TransferPostsResponseDto getSourcerAllPosts(TransferPostSource sourcer);
}
