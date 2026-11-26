package com.raktim.search_service.projection;

import java.time.Duration;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.raktim.search_service.client.CatalogueClient;
import com.raktim.search_service.client.CatalogueProduct;
import com.raktim.search_service.common.web.RequestIdFilter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Copies every product from catalogue into the store: at startup if it started empty, or on request.
 * At startup it keeps trying until catalogue answers, because catalogue may still be starting.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductReloader {

    private static final Duration RETRY_EVERY = Duration.ofSeconds(5);

    private final CatalogueClient catalogue;
    private final ProductStore store;

    private volatile boolean loaded;              // true once the store holds catalogue's products
    private volatile int attempts;                // startup tries so far, shown in /actuator/health

    /** Once, when search is ready. The retries run on their own thread, so startup isn't held up. */
    @EventListener(ApplicationReadyEvent.class)
    public void reloadIfStartedEmpty() {
        if (!store.startedEmpty()) {
            loaded = true;
            log.info("[reload] the index already held products at startup, catalogue not called");
            return;
        }
        Thread.ofVirtual().name("startup-reload").start(this::reloadUntilDone);
    }

    /** Tries every 5 s until a reload works: this one, or a POST /actuator/reload in between. */
    private void reloadUntilDone() {
        while (!loaded) {
            attempts++;
            try {
                reload();
            } catch (RuntimeException e) {             // catalogue down or still starting
                log.warn("[reload] startup attempt {} failed: {}. Trying again in {} s",
                        attempts, e.getMessage(), RETRY_EVERY.toSeconds());
                try {
                    Thread.sleep(RETRY_EVERY);
                } catch (InterruptedException stop) {  // search is shutting down
                    return;
                }
            }
        }
    }

    /** Reads every product from catalogue. The store keeps only versions newer than the ones it holds. */
    public Result reload() {
        MDC.put(RequestIdFilter.MDC_KEY, "reload-" + UUID.randomUUID().toString().substring(0, 8));
        long started = System.nanoTime();
        try {
            int applied = 0;
            int skipped = 0;
            for (CatalogueProduct product : catalogue.fetchAll()) {
                if (store.apply(ProductDocument.from(product))) {
                    applied++;
                } else {
                    skipped++;                        // already current
                }
            }
            loaded = true;                            // also ends the startup retries
            long tookMs = (System.nanoTime() - started) / 1_000_000;
            log.info("[reload] {} applied, {} skipped in {} ms", applied, skipped, tookMs);
            return new Result(applied, skipped, tookMs);
        } finally {
            MDC.remove(RequestIdFilter.MDC_KEY);
        }
    }

    /** True once catalogue's products are in the store. */
    public boolean loaded() {
        return loaded;
    }

    /** How many times the startup reload has tried so far. */
    public int attempts() {
        return attempts;
    }

    /** What a reload did. Also the JSON answer of POST /actuator/reload. */
    public record Result(int applied, int skipped, long tookMs) {
    }
}