package com.raktim.search_service.client;

import java.io.IOException;

import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import com.raktim.search_service.common.web.RequestIdFilter;

/** Copies the current request id onto every call to catalogue, so both logs share it. */
public class RequestIdPropagation implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        String requestId = MDC.get(RequestIdFilter.MDC_KEY);
        if (requestId != null) {
            request.getHeaders().set(RequestIdFilter.HEADER, requestId);
        }
        return execution.execute(request, body);
    }
}