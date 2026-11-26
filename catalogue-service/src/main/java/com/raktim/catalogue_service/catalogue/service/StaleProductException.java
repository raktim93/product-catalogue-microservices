package com.raktim.catalogue_service.catalogue.service;

import java.util.Map;

import com.raktim.catalogue_service.common.error.ConflictException;

/** The client edited an older copy of the product than the one now stored. */
public class StaleProductException extends ConflictException {

    private final long currentVersion;

    public StaleProductException(long productId, long expectedVersion, long currentVersion) {
        super("Product " + productId + " is at version " + currentVersion
                + ", but the request was based on version " + expectedVersion + ".");
        this.currentVersion = currentVersion;
    }

    @Override
    public Map<String, Object> details() {
        return Map.of("currentVersion", currentVersion);
    }
}