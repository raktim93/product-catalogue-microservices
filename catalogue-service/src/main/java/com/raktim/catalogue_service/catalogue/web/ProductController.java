package com.raktim.catalogue_service.catalogue.web;

import java.net.URI;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.raktim.catalogue_service.catalogue.domain.Product;
import com.raktim.catalogue_service.catalogue.service.PriceChange;
import com.raktim.catalogue_service.catalogue.service.ProductService;
import com.raktim.catalogue_service.common.web.PageResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService service;

    @GetMapping
    public PageResponse<ProductResponse> list(@RequestParam(required = false) String category,
                                              @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return PageResponse.from(service.list(category, pageable).map(ProductResponse::from));
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable long id) {
        return ProductResponse.from(service.get(id));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        Product created = service.create(request.name(), request.description(),   // name(), not getName()
                request.category(), request.price());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(ProductResponse.from(created));
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable long id, @Valid @RequestBody UpdateProductRequest request) {
        Product updated = service.update(id, request.version(), request.name(),
                request.description(), request.category(), request.price());
        return ProductResponse.from(updated);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        service.delete(id);
    }

    @PostMapping("/price-adjustments")
    public List<ProductResponse> adjustPrices(@Valid @RequestBody PriceAdjustmentRequest request) {
        List<PriceChange> changes = request.adjustments().stream()
                .map(item -> new PriceChange(item.productId(), item.newPrice()))
                .toList();
        return service.adjustPrices(changes).stream()
                .map(ProductResponse::from)
                .toList();
    }
}