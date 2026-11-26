package com.raktim.search_service.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** catalogue.client.* from application.properties. A record binds through its constructor. */
@ConfigurationProperties(prefix = "catalogue.client")
public record CatalogueClientProperties(String baseUrl, Duration connectTimeout,
    Duration readTimeout, int pageSize) {

}
