package com.raktim.catalogue_service.catalogue.events;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

/** Sends a product event to Kafka. Key = product id, value = the event as JSON. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventPublisher {

    /*private final KafkaTemplate<String, String> kafka;
    private final JsonMapper json;                         // Spring Boot 4 uses Jackson 3 (package tools.jackson)

    @Value("${catalogue.events.topic}")
    private final String topic;

    public void publish(ProductEvent event) {
        String key = String.valueOf(event.productId());
        kafka.send(topic, key, json.writeValueAsString(event))
                .whenComplete((result, error) -> {          // send() doesn't wait; this runs when Kafka answers
                    if (error != null) {
                        log.error("[events] {} v{} for product {} NOT sent: {}",
                                event.type(), event.version(), key, error.getMessage());
                    } else {
                        log.info("[events] {} v{} for product {} -> partition {} offset {}", event.type(),
                                event.version(), key, result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }*/

    private final OutboxEventRepository outbox;
    private final JsonMapper json;

    public void publish(ProductEvent event) {
        outbox.save(new OutboxEvent(event.eventId(), event.productId(), event.type(),
                json.writeValueAsString(event)));
        log.info("[outbox] {} v{} for product {} saved", event.type(), event.version(), event.productId());
    }
}