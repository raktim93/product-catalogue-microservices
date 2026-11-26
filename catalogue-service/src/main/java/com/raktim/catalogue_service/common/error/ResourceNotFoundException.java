package com.raktim.catalogue_service.common.error;

/** Base class for "the thing you asked for doesn't exist": mapped to 404. */
public abstract class ResourceNotFoundException extends RuntimeException {

    protected ResourceNotFoundException(String message) {
        super(message);
    }
}