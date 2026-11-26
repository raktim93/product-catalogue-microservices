package com.raktim.api_gateway.fallback;

import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import lombok.extern.slf4j.Slf4j;

/** Where the catalogue route goes when its breaker says no. Answers at once and never calls catalogue. */
@Slf4j
@RestController
public class FallbackController {

    @RequestMapping("/fallback/catalogue")
    public ResponseEntity<ProblemDetail> catalogueUnavailable(ServerWebExchange exchange) {
        // the exception that sent us here, put on the exchange by the CircuitBreaker filter
        Throwable cause = exchange.getAttribute(ServerWebExchangeUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR);
        String reason = cause == null ? "unknown" : cause.getClass().getSimpleName();
        String requestId = exchange.getRequest().getHeaders().getFirst("X-Request-Id");   // null before 4.5
        log.warn("[{}] catalogue fallback, reason: {}", requestId, reason);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Catalogue is not available right now. Try again in a few seconds.");
        problem.setTitle("Catalogue unavailable");
        problem.setProperty("reason", reason);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, "15")                // seconds, the breaker's wait
                .body(problem);
        
    }
}