package com.dhoon.transfertracker.internal.post.sync.event;

import java.util.List;

public record PostCreatedEvent(List<PostEventData> posts) {

}
