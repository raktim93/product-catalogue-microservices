package com.raktim.catalogue_service.catalogue.events;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /** Not sent yet, oldest first. */
    List<OutboxEvent> findTop100ByPublishedAtIsNullOrderByIdAsc();
}