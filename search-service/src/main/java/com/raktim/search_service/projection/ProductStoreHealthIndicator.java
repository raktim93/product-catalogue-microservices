package com.raktim.search_service.projection;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/** "productStore" in /actuator/health and in the readiness group: is the index loaded and answering? */
@Component
@RequiredArgsConstructor
public class ProductStoreHealthIndicator implements HealthIndicator {

    private final ProductStore store;
    private final ProductReloader reloader;                  // knows whether the startup reload has finished

    @Override
    public Health health() {
        if (!reloader.loaded()) {                            // started empty, catalogue not copied yet
            return Health.outOfService()
                    .withDetail("reload", "waiting for catalogue, attempts so far: " + reloader.attempts())
                    .build();
        }
        try {
            return Health.up()
                    .withDetail("products", store.count())
                    .build();
        } catch (RuntimeException e) {                      // Elasticsearch down or not answering
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}