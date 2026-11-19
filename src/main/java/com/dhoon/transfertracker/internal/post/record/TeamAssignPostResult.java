package com.dhoon.transfertracker.internal.post.record;

import java.util.List;

public record TeamAssignPostResult (
        Long postId,
        List<String> teamNames,
        List<String> playerNames
){
}
