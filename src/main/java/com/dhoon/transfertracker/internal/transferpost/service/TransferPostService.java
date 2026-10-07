package com.dhoon.transfertracker.internal.transferpost.service;

import com.dhoon.transfertracker.external.openai.dto.response.TranslationBatchResult;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transfer.dto.response.TransferPostsResponseDto;

public interface TransferPostService {

    TransferPostsResponseDto getSourcerAllPosts(TransferPostSource sourcer);

    String postAssignTeam();

    // 외부 API ID 가 아닌, 사용중인 DB 안에있는 team_id
    TransferPostsResponseDto getTeamTransferPosts(Long teamId);

    TransferPostsResponseDto getAllTransferPosts();


}
