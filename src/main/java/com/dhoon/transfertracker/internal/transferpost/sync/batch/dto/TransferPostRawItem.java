package com.dhoon.transfertracker.internal.transferpost.sync.batch.dto;

import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import tools.jackson.databind.JsonNode;

import java.util.Map;

public record TransferPostRawItem (
        TransferPostSource source,
        JsonNode post,
        Map<String, String> mediaUrlByKey
) {}


