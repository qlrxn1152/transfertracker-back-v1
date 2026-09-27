package com.dhoon.transfertracker.external.x.controller;

import com.dhoon.transfertracker.external.x.client.XClient;
import com.dhoon.transfertracker.internal.transferpost.domain.TransferPostSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
@Slf4j
@RequiredArgsConstructor
public class XController {

    private final XClient xClient;

    @PostMapping("/external/api/posts/{source}")
    public String syncPosts(@PathVariable TransferPostSource source) {
        return xClient.syncPosts(source);
    }

    @GetMapping("/test/{username}")
    public JsonNode test2(@PathVariable String username) {
        JsonNode a = xClient.getUserByUsername(username);

        return a;
    }




}
