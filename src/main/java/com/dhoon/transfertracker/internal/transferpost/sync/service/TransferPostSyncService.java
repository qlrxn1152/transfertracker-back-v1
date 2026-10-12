package com.dhoon.transfertracker.internal.transferpost.sync.service;

import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostsResponseDto;

public interface TransferPostSyncService {

    TransferPostsResponseDto syncPosts(TransferPostSource source);
}
