package com.raktim.search_service.events;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import com.raktim.search_service.projection.IndexUnavailableException;

import tools.jackson.core.JacksonException;

/** When ProductEventListener throws: wait for Elasticsearch, or retry briefly and park the message in the DLT. */
@Configuration
public class KafkaErrorConfig {

    /** The dead-letter topic. Same partition count as product-events, because a failed message keeps its partition. */
    @Bean
    NewTopic productEventsDlt(@Value("${search.events.topic}") String topic) {
        return TopicBuilder.name(topic + ".DLT").partitions(3).replicas(1).build();
    }

    /** Spring Boot puts this handler on every @KafkaListener. */
    @Bean
    DefaultErrorHandler errorHandler(KafkaTemplate<String, String> template) {
        DeadLetterPublishingRecoverer toDlt = new DeadLetterPublishingRecoverer(template,
                (message, error) -> new TopicPartition(message.topic() + ".DLT", message.partition()));
        DefaultErrorHandler handler = new DefaultErrorHandler(toDlt, new FixedBackOff(1000L, 2));   // 2 retries, 1 s apart
        handler.addNotRetryableExceptions(JacksonException.class,           // not JSON, or a required field missing
                IllegalArgumentException.class);                            // an event type search doesn't know
        // CAT-68: Elasticsearch down is not the message's fault. Retry every 5 s until it is back, never the DLT.
        handler.setBackOffFunction((record, exception) -> causedBy(exception, IndexUnavailableException.class)
                ? new FixedBackOff(5000L, FixedBackOff.UNLIMITED_ATTEMPTS)
                : null);                                                    // null = the 2 retries above
        return handler;
    }

    /** Spring wraps what the listener threw (ListenerExecutionFailedException), so look down the cause chain. */
    private static boolean causedBy(Throwable exception, Class<? extends Throwable> type) {
        for (Throwable current = exception; current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return true;
            }
        }
        return false;
    }
}