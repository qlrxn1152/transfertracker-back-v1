package com.dhoon.transfertracker.internal.post.sync.event;

import com.dhoon.transfertracker.internal.post.sync.service.PostProcessingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


import lombok.RequiredArgsConstructor;

@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class PostCreatedConsumer {

    private final PostProcessingService postProcessingService;

    @KafkaListener(topics = "veyro.post.created")
    public void consume(PostCreatedEvent event) {
        log.info("게시물 생성 이벤트 수신. count={}",
                event.posts().size());

        postProcessingService.process(event.posts());


    }

}
