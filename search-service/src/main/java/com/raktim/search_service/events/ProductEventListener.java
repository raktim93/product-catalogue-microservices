package com.raktim.search_service.events;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.raktim.search_service.projection.ProductDocument;
import com.raktim.search_service.projection.ProductStore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

/** Applies catalogue's product events to search's copy of the products. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventListener {

    private final ProductStore store;              // CAT-67: was ProductProjection
    private final JsonMapper json;

    @KafkaListener(topics = "${search.events.topic}")
    public void on(ConsumerRecord<String, String> record) {
        ProductEvent event = json.readValue(record.value(), ProductEvent.class);
        boolean changed = switch (event.type()) {
            case ProductEvent.CREATED, ProductEvent.UPDATED -> store.apply(ProductDocument.from(event));
            case ProductEvent.DELETED -> store.remove(event.productId(), event.version());
            default -> throw new IllegalArgumentException("Unknown event type " + event.type());
        };
        log.info("[events] partition {} offset {}: {} v{} for product {} -> {}", record.partition(),
                record.offset(), event.type(), event.version(), event.productId(), changed ? "applied" : "skipped");
    }
}