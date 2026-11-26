package com.raktim.catalogue_service.catalogue.persistence;

import com.raktim.catalogue_service.catalogue.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

/**
 * No implementation class: Spring Data creates one at startup.
 * JpaRepository already provides save, findById, findAll(Pageable), delete, count, ...
 */

public interface ProductRepository extends JpaRepository<Product,Long> {

    // Derived query: Spring Data reads the method name and generates
    // "select p from Product p where p.category = ?1", plus a count query for the page
    Page<Product> findByCategory(String category, Pageable pageable);

    // JPQL (entity and field names, not table and column names). A null parameter switches its filter off.
    @Query("""
            select p from Product p
            where (:pattern is null
                   or lower(p.name) like :pattern
                   or lower(p.description) like :pattern)
              and (:category is null or p.category = :category)
              and (:minPrice is null or p.price >= :minPrice)
              and (:maxPrice is null or p.price <= :maxPrice)
            """)
    Page<Product> search(@Param("pattern") String pattern,
                         @Param("category") String category,
                         @Param("minPrice") BigDecimal minPrice,
                         @Param("maxPrice") BigDecimal maxPrice,
                         Pageable pageable);
}
