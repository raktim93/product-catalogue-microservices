package com.raktim.search_service.projection;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Elasticsearch didn't answer, or answered with a 5xx. Temporary: worth waiting for, unlike a bad message. */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)          // a search request that meets it gets 503, not 500
public class IndexUnavailableException extends RuntimeException{
    public IndexUnavailableException(Exception cause) {
        super("Elasticsearch is not available: " + cause.getMessage(), cause);
    }
}
