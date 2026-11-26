package com.raktim.api_gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Gives every request an id before routing, passes it to the service, returns it to the client and
 * logs one line per request. Reactive: the id is passed into the log call, not kept in MDC.
 */

@Slf4j
@Component
public class RequestIdGlobalFilter implements GlobalFilter, Ordered {

    private static final String HEADER = "X-Request-Id";
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9-]{1,64}");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(HEADER);
        String requestId = incoming != null && SAFE_ID.matcher(incoming).matches() ? incoming
                : UUID.randomUUID().toString();
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> headers.set(HEADER, requestId))      // to the service
                .build();
        exchange.getResponse().getHeaders().set(HEADER, requestId);      // back to the client
        long started = System.nanoTime();
        return chain.filter(exchange.mutate().request(request).build())
                .doFinally(signal -> log.info("[{}] {} {} -> {} in {} ms", requestId,
                        request.getMethod(), request.getURI().getRawPath(),
                        exchange.getResponse().getStatusCode(), (System.nanoTime() - started) / 1_000_000));

    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
