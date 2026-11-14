package com.dhoon.transfertracker.internal.transferpost.sync.service.impl;

import com.dhoon.transfertracker.external.x.client.XClient;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostItemResponseDto;
import com.dhoon.transfertracker.internal.transferpost.dto.response.TransferPostsResponseDto;
import com.dhoon.transfertracker.internal.transferpost.exception.InvalidSourcerException;
import com.dhoon.transfertracker.internal.transferpost.sync.dto.response.TransferPostSyncResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Service
public class TransferPostSyncOrchestrationService {

    private final TransferPostSyncTxService txService;
    private final XClient xClient;

    public TransferPostSyncResponseDto syncPosts(TransferPostSource source) {
        String nextToken = null;
        List<TransferPostItemResponseDto> posts = new ArrayList<>();

        do {
            JsonNode response = xClient.callExternalApi(source, nextToken); // null -> 첫페이지.
            JsonNode userPosts = response.get("data");

            if (userPosts == null) {
                throw new InvalidSourcerException();
            }

            Map<String, String> mediaUrlByKey = mapMediaUrlByKey(response);

            userPosts.forEach(post -> {
                TransferPostItemResponseDto saveTransferPost = txService.saveTransferPost(source, post, mediaUrlByKey);
                posts.add(saveTransferPost);
            });

            nextToken = extractNextToken(response);
        }
        while (nextToken != null && !nextToken.isBlank());

        return TransferPostSyncResponseDto.of(posts);
    }


    //  ============================ //

    private Map<String, String> mapMediaUrlByKey(JsonNode response) {
        Map<String, String> mediaUrlByKey = new HashMap<>();
        JsonNode includes = response.get("includes");

        if (includes == null) {
            return mediaUrlByKey;
        }

        JsonNode mediaList = includes.get("media");

        if (mediaList == null) {
            return mediaUrlByKey;
        }

        mediaList.forEach(media -> {
            String mediaKey = getString(media, "media_key");
            String type = getString(media, "type");

            if (mediaKey == null || type == null) {
                return;
            }

            String mediaUrl =
                    "photo".equals(type)
                            ? getString(media, "url")
                            : getString(media, "preview_image_url");

            if (mediaUrl != null && !mediaUrl.isBlank()) {
                mediaUrlByKey.put(mediaKey, mediaUrl);
            }
        });

        return mediaUrlByKey;
    }


    private String getString(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);

        if (value == null) {
            return null;
        }

        return value.asString();
    }

    private String extractNextToken(JsonNode response) {
        JsonNode meta = response.get("meta");

        String nextToken = null;

        if (meta != null && meta.hasNonNull("next_token")) {
            nextToken = meta.get("next_token").asString();
        }

        return nextToken;
    }

}
