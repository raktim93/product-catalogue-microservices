package com.raktim.catalogue_service.catalogue.events;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Sends unsent outbox rows to Kafka, oldest first, and marks each one sent. */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outbox;
    private final KafkaTemplate<String, String> kafka;

    @Value("${catalogue.events.topic}")
    private final String topic;

    @Scheduled(fixedDelayString = "${catalogue.outbox.poll-ms}")
    public void publishPending() {
        for (OutboxEvent row : outbox.findTop100ByPublishedAtIsNullOrderByIdAsc()) {
            try {
                SendResult<String, String> sent = kafka
                        .send(topic, String.valueOf(row.getAggregateId()), row.getPayload())
                        .get();                                    // wait until Kafka has stored it
                row.markPublished();
                outbox.save(row);
                log.info("[outbox] sent row {} ({} for product {}) -> partition {} offset {}", row.getId(),
                        row.getEventType(), row.getAggregateId(),
                        sent.getRecordMetadata().partition(), sent.getRecordMetadata().offset());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();                // the app is shutting down
                return;
            } catch (Exception e) {
                log.warn("[outbox] row {} not sent, will retry: {}", row.getId(), e.getMessage());
                return;                                            // stop here: keep the order
            }
        }
    }
}