package com.raktim.api_gateway.config;

import java.time.Duration;

import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import lombok.extern.slf4j.Slf4j;

/** Settings for the breaker named "catalogue" (the name in application.yaml), plus a log line on every state change. */
@Slf4j
@Configuration
public class CatalogueBreakerConfig {

    private static final String NAME = "catalogue";

    @Bean
    Customizer<ReactiveResilience4JCircuitBreakerFactory> catalogueBreaker() {
        CircuitBreakerConfig breaker = CircuitBreakerConfig.custom()
                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)                                // judge the last 10 calls
                .minimumNumberOfCalls(5)                              // but only once there are at least 5
                .failureRateThreshold(50)                             // open when half of them failed
                .slowCallDurationThreshold(Duration.ofSeconds(1))     // a call over 1 s counts as slow
                .slowCallRateThreshold(50)                            // open when half of them were slow
                .waitDurationInOpenState(Duration.ofSeconds(15))      // stay open for 15 s
                .automaticTransitionFromOpenToHalfOpenEnabled(true)   // then go half-open, even with no traffic
                .permittedNumberOfCallsInHalfOpenState(3)             // 3 trial calls decide: close or reopen
                .build();
        TimeLimiterConfig timeLimit = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(2))               // a call over 2 s is cut off: a failure
                .build();
        // once: the factory runs customizers on every request; without it, each request would add another listener
        Customizer<CircuitBreaker> logStateChanges = Customizer.once(
                circuitBreaker -> circuitBreaker.getEventPublisher().onStateTransition(event ->
                        log.warn("[breaker] {}: {}", event.getCircuitBreakerName(), event.getStateTransition())),
                CircuitBreaker::getName);
        return factory -> {
            factory.configure(builder -> builder
                    .circuitBreakerConfig(breaker)
                    .timeLimiterConfig(timeLimit), NAME);
            factory.addCircuitBreakerCustomizer(logStateChanges, NAME);
        };
    }
}