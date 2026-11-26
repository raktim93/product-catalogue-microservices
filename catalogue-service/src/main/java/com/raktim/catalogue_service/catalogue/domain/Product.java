    package com.raktim.catalogue_service.catalogue.domain;

    import jakarta.persistence.*;
    import lombok.*;

    import java.math.BigDecimal;

    /**
     * JPA entity: one row of the "products" table, managed by Hibernate inside a transaction.
     * Never returned to clients directly: the web layer maps it to ProductResponse.
     */

    @Entity
    @Table(name = "products",
            uniqueConstraints = @UniqueConstraint(name = "uk_products_name", columnNames = "name"),
            indexes = @Index(name = "idx_products_category", columnList = "category"))
    @Getter           // a getter for every field
    @NoArgsConstructor(access = AccessLevel.PROTECTED) // for JPA only: protected Product() {}
    public class Product {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)   // the database assigns the id during INSERT
        private Long id;

        @Setter
        @Column(nullable = false, length = 120)
        private String name;

        @Setter
        @Column(length = 2000)
        private String description;

        @Setter
        @Column(nullable = false, length = 50)
        private String category;

        @Setter
        @Column(nullable = false, precision = 12, scale = 2)  // numeric(12, 2): exact decimal, never double
        private BigDecimal price;

        @Version                                             // optimistic locking: incremented on every UPDATE (CP 2.6)
        private Long version;

        public Product(String name, String description, String category, BigDecimal price) {
            this.name = name;
            this.description = description;
            this.category = category;
            this.price = price;
        }

        // No setters for id and version: the database and Hibernate own them.
    }
