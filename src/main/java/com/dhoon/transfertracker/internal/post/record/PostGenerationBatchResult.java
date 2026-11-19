package com.dhoon.transfertracker.internal.post.record;

import java.util.List;

public record PostGenerationBatchResult (
        List<PostGenerationResult> posts
){
}
