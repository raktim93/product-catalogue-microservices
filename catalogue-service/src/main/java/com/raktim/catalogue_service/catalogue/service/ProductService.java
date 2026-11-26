package com.raktim.catalogue_service.catalogue.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.raktim.catalogue_service.catalogue.domain.Product;
import com.raktim.catalogue_service.catalogue.events.ProductEvent;
import com.raktim.catalogue_service.catalogue.events.ProductEventPublisher;
import com.raktim.catalogue_service.catalogue.persistence.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)                 // default for every public method in this class
public class ProductService {

    private final ProductRepository repository;
    private final ProductEventPublisher events;                    // CAT-61

    public Page<Product> list(String category, Pageable pageable) {
        if (category == null || category.isBlank()) {
            return repository.findAll(pageable);
        }
        return repository.findByCategory(normalizeCategory(category), pageable);
    }

    public Product get(long id) {
        return repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Transactional
    public Product create(String name, String description, String category, BigDecimal price) {
        Product product = new Product(name.trim(), description, normalizeCategory(category), price);
        Product saved = repository.save(product);                  // the INSERT runs now, version is 0
        events.publish(ProductEvent.created(saved));               // CAT-61
        log.info("Created product {} '{}'", saved.getId(), saved.getName());
        return saved;
    }

    @Transactional
    public Product update(long id, long expectedVersion, String name, String description,
                          String category, BigDecimal price) {
        Product product = get(id);
        if (product.getVersion() != expectedVersion) {             // the client edited an old copy
            throw new StaleProductException(id, expectedVersion, product.getVersion());
        }
        product.setName(name.trim());
        product.setDescription(description);
        product.setCategory(normalizeCategory(category));
        product.setPrice(price);
        repository.flush();                                        // CAT-61: run the UPDATE now, so getVersion() is the new one
        events.publish(ProductEvent.updated(product));             // CAT-61
        return product;
    }

    @Transactional
    public void delete(long id) {
        Product product = get(id);
        repository.delete(product);
        repository.flush();                                        // CAT-61: the DELETE must succeed before we announce it
        events.publish(ProductEvent.deleted(product));             // CAT-61
        log.info("Deleted product {}", id);
    }

    @Transactional
    public List<Product> adjustPrices(List<PriceChange> changes) {
        List<Product> updated = new ArrayList<>();
        for (PriceChange change : changes) {
            Product product = get(change.productId());
            product.setPrice(change.newPrice());
            updated.add(repository.save(product));
            log.info("[tx] product {} price set to {}", product.getId(), change.newPrice());
        }
        repository.flush();                                        // CAT-61: every UPDATE runs, every version moves on
        updated.forEach(product -> events.publish(ProductEvent.updated(product)));   // CAT-61
        return updated;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<Product> adjustPricesViaSelfCall(List<PriceChange> changes) {
        return adjustPrices(changes);
    }

    static String normalizeCategory(String category) {
        return category.trim().toUpperCase(Locale.ROOT);
    }
}