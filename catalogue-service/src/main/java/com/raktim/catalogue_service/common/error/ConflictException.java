package com.raktim.catalogue_service.common.error;

import java.util.Map;

/** Base class for "the request conflicts with the current state": mapped to 409. */
public abstract class ConflictException extends RuntimeException {

    protected ConflictException(String message) {
        super(message);
    }

    /** Extra fields for the error response. Override to add some. */
    public Map<String, Object> details() {
        return Map.of();
    }
}