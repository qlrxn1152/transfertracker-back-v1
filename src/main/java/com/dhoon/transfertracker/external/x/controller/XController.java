package com.dhoon.transfertracker.external.x.controller;

import com.dhoon.transfertracker.external.x.client.XClient;
import com.dhoon.transfertracker.internal.domain.TransferPostSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
@Slf4j
@RequiredArgsConstructor
public class XController {

    private final XClient xClient;

    @GetMapping("/external/api/posts/{source}")
    public String test(@PathVariable TransferPostSource source) {
        return xClient.syncPosts(source);
    }

}
