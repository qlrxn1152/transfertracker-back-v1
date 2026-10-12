package com.dhoon.transfertracker.internal.transferpost.sync.service;

import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostItemResponseDto;
import tools.jackson.databind.JsonNode;

import java.util.Map;

public interface TransferPostSyncTxService {

    TransferPostItemResponseDto  saveTransferPost(TransferPostSource source, JsonNode post, Map<String, String> mediaUrlByKey);
}
