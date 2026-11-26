package com.raktim.search_service.projection;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/** POST /actuator/reload, on search's own port. An operations tool: not under /api, so the gateway never routes it. */
@Component
@Endpoint(id = "reload")
@RequiredArgsConstructor
public class ReloadEndpoint {

    private final ProductReloader reloader;

    @WriteOperation
    public ProductReloader.Result reload() {
        return reloader.reload();
    }
}