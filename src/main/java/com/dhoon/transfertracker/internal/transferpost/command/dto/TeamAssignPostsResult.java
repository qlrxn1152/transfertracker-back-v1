package com.dhoon.transfertracker.internal.transferpost.command.dto;

import java.util.List;

public record TeamAssignPostsResult(
        List<TeamAssignPostResult> posts
) {
}
