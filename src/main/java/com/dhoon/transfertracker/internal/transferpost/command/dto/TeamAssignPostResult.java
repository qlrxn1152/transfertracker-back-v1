package com.dhoon.transfertracker.internal.transferpost.command.dto;

import java.util.List;

public record TeamAssignPostResult (
        Long postId,
        List<String> teamNames
){
}
