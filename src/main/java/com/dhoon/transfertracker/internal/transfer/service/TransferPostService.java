package com.dhoon.transfertracker.internal.transfer.service;

import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transfer.dto.response.TransferPostsResponseDto;

public interface TransferPostService {

    TransferPostsResponseDto getSourcerAllPosts(TransferPostSource sourcer);
}
