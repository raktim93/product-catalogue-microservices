package com.raktim.catalogue_service.catalogue.service;


import com.raktim.catalogue_service.common.error.ResourceNotFoundException;

public class ProductNotFoundException extends ResourceNotFoundException {

    private final long productId;

    public ProductNotFoundException(long productId) {
        super("Product " + productId + " not found");
        this.productId = productId;
    }

    public long getProductId() {
        return productId;
    }

}
