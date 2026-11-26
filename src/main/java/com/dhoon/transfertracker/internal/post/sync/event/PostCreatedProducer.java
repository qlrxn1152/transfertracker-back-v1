package com.dhoon.transfertracker.internal.post.sync.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("local")
public class PostCreatedProducer {

    private final KafkaTemplate<String, PostCreatedEvent> kafkaTemplate;

    public void send(PostCreatedEvent event) {
        kafkaTemplate.send("veyro.post.created", event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("게시물 생성 이벤트 발행 실패", ex);
                        return;
                    }

                    log.info("게시물 생성 이벤트 발행 성공. count={}, partition={}, offset={}",
                            event.posts().size(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                });
    }
}
