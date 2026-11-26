package com.raktim.search_service.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Gives every request an id: reuses the caller's X-Request-Id if it looks safe, otherwise creates one.
 * Runs first, so every later step can read it.
 */

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String ATTRIBUTE = RequestIdFilter.class.getName() + ".requestId";
    public static final String MDC_KEY = "requestId";

    // Letters, digits and dashes only: the value ends up in logs and responses, so never trust it blindly
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String incoming = request.getHeader(HEADER);
        String requestId = incoming != null && SAFE_ID.matcher(incoming).matches()
                ? incoming
                : UUID.randomUUID().toString();
        request.setAttribute(ATTRIBUTE, requestId);
        response.setHeader(HEADER, requestId);
        MDC.put(MDC_KEY, requestId);              // every log line on this thread now carries the id
        long started = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            // before MDC.remove, so this line still carries the id
            log.info("{} {} -> {} in {} ms", request.getMethod(), request.getRequestURI(),
                    response.getStatus(), (System.nanoTime() - started) / 1_000_000);
            MDC.remove(MDC_KEY);                  // Tomcat reuses threads: never leave an id behind
        }
    }
}