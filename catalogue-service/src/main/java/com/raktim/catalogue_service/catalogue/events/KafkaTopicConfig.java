package com.raktim.catalogue_service.catalogue.events;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Topics catalogue owns. KafkaAdmin creates each one at startup if it doesn't exist yet. */
@Configuration
public class KafkaTopicConfig {
    @Bean
    NewTopic productEventsTopic(@Value("${catalogue.events.topic}") String topic) {
        return TopicBuilder.name(topic)
                .partitions(3)       // up to 3 search instances in one group can share the work
                .replicas(1)         // one broker here; 3 in production
                .build();
    }
}

