package com.dhoon.transfertracker.internal.transferpost.sync.batch.reader;

import com.dhoon.transfertracker.external.x.client.XClient;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import com.dhoon.transfertracker.internal.transferpost.sync.batch.dto.TransferPostRawItem;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Component
@StepScope
@RequiredArgsConstructor
public class TransferPostItemReader implements ItemReader<TransferPostRawItem> {

    private final XClient xClient;

    private final TransferPostSource source =
            TransferPostSource.FABRIZIO_ROMANO;

    private Iterator<JsonNode> postIterator;
    private Map<String, String> mediaUrlByKey = new HashMap<>();
    private String nextToken;
    private boolean finished = false;

    private static final int MAX_PAGES = 2;
    private int pageCount = 0;

    @Override
    public TransferPostRawItem read() throws Exception {

        if (finished) {
            return null;
        }

        while (true) {

            if (postIterator != null && postIterator.hasNext()) {
                JsonNode post = postIterator.next();

                return new TransferPostRawItem(
                        source, post, mediaUrlByKey
                );
            }

            if (postIterator != null && (nextToken == null || nextToken.isBlank())) {
                finished = true;
                return null;
            }

            if (pageCount >= MAX_PAGES) {
                finished = true;
                return null;
            }

            loadPage();

        }

    }




    private void loadPage() {
        JsonNode response = xClient.callExternalApi(source, nextToken);

        pageCount++;

        if (response == null || !response.isObject()) {
            throw new IllegalStateException("X API 응답이 올바르지 않습니다.");
        }

        mediaUrlByKey = mapMediaUrlByKey(response);

        JsonNode userPosts = response.get("data");

        if ( userPosts == null || !userPosts.isArray() ) {
            throw new IllegalStateException("X API 게시물 데이터가 올바르지 않습니다.");
        }

        postIterator = userPosts.iterator();

        JsonNode meta = response.get("meta");

        if (meta != null && meta.hasNonNull("next_token")) {
            nextToken = meta.get("next_token").asString();
        } else {
            nextToken = null;
        }
    }



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

        if (value == null || value.isNull()) {
            return null;
        }

        return value.asString();
    }
}