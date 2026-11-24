package com.dhoon.transfertracker.internal.post.domain;

public enum PostSyncStep {
    ASSIGN,
    TRANSLATE,
    NOTIFICATION;

    public PostSyncStep next() {
        return switch (this) {
            case ASSIGN -> TRANSLATE;
            case TRANSLATE -> NOTIFICATION;
            default -> null;
        };

    }

}
