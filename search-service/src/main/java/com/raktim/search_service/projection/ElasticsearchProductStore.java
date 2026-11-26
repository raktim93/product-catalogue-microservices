package com.raktim.search_service.projection;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.raktim.search_service.common.web.PageResponse;
import com.raktim.search_service.search.ProductSearchCriteria;
import com.raktim.search_service.search.ProductSummary;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.VersionType;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Search's copy of the products: the Elasticsearch index "products". The only ProductStore since CAT-72. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchProductStore implements ProductStore {

    public static final String INDEX = "products";
    private static final String RELEVANCE = "relevance";
    /** ?sort= value -> the field Elasticsearch sorts on. name is text, so it sorts on its keyword copy. */
    private static final Map<String, String> SORT_FIELDS = Map.of("id", "id", "name", "name.sort", "price", "price");
    private static final int MAX_RESULT_WINDOW = 10_000;      // Elasticsearch's default limit for from + size

    private final ElasticsearchClient client;
    private boolean startedEmpty;

    @Override
    public boolean startedEmpty() {
        return startedEmpty;
    }

    /** Runs while the beans are created, before the Kafka listener starts. */
    @PostConstruct
    void createIfMissing() {
        try {
            if (client.indices().exists(e -> e.index(INDEX)).value()) {
                startedEmpty = count() == 0;
                log.info("[index] {} already exists", INDEX);
                return;
            }
            try (InputStream definition = new ClassPathResource("elasticsearch/products.json").getInputStream()) {
                client.indices().create(c -> c.index(INDEX).withJson(definition));
            }
            startedEmpty = true;
            log.info("[index] created {} from elasticsearch/products.json", INDEX);
        } catch (IOException e) {
            throw new IndexUnavailableException(e);       // Elasticsearch not reachable: startup stops here
        }
    }

    @Override
    public boolean apply(ProductDocument document) {
        return write(document);
    }

    @Override
    public boolean remove(long id, long version) {
        return write(ProductDocument.tombstone(id, version));
    }

    @Override
    public long count() {
        try {
            return client.count(c -> c.index(INDEX)
                    .query(q -> q.term(t -> t.field("deleted").value(false)))).count();
        } catch (IOException e) {
            throw new IndexUnavailableException(e);
        }
    }

    @Override
    public PageResponse<ProductSummary> search(ProductSearchCriteria criteria, int page, int size, String sort) {
        if ((long) (page + 1) * size > MAX_RESULT_WINDOW) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "(page + 1) * size must not exceed " + MAX_RESULT_WINDOW + ". Narrow the search instead.");
        }
        String[] parts = sort.split(",");
        String key = parts[0].trim();
        boolean byRelevance = key.equals(RELEVANCE);
        if (!byRelevance && !SORT_FIELDS.containsKey(key)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot sort by '" + key + "'. Use relevance, id, name or price.");
        }
        // relevance: best match first unless ",asc" is given. The others: ascending unless ",desc" is given.
        boolean descending = parts.length > 1 ? parts[1].trim().equalsIgnoreCase("desc") : byRelevance;
        SortOrder order = descending ? SortOrder.Desc : SortOrder.Asc;

        SearchResponse<ProductDocument> response;
        try {
            response = client.search(s -> s
                            .index(INDEX)
                            .query(q -> q.bool(b -> filters(b, criteria)))
                            .sort(o -> byRelevance
                                    ? o.score(sc -> sc.order(order))
                                    : o.field(f -> f.field(SORT_FIELDS.get(key)).order(order)))
                            .sort(o -> o.field(f -> f.field("id").order(SortOrder.Asc)))   // tie-breaker: stable pages
                            .from(page * size)
                            .size(size)
                            .trackTotalHits(t -> t.enabled(true)),                       // an exact total, not "10,000+"
                    ProductDocument.class);
        } catch (IOException e) {
            throw new IndexUnavailableException(e);
        }

        List<ProductSummary> content = response.hits().hits().stream()
                .map(Hit::source)
                .map(p -> new ProductSummary(p.id(), p.name(), p.category(), p.price()))
                .toList();
        long total = response.hits().total() == null ? content.size() : response.hits().total().value();
        int totalPages = (int) ((total + size - 1) / size);
        return new PageResponse<>(content, page, size, total, totalPages);
    }

    private boolean write(ProductDocument document) {
        try {
            client.index(i -> i
                    .index(INDEX)
                    .id(String.valueOf(document.id()))
                    .version(document.version())
                    .versionType(VersionType.External)    // stored only if newer than the version Elasticsearch holds
                    .document(document));
            return true;
        } catch (ElasticsearchException e) {
            if (e.status() == 409) {                      // this version or a newer one is already there
                return false;
            }
            if (e.status() >= 500) {                      // Elasticsearch is up but not able to work yet
                throw new IndexUnavailableException(e);
            }
            throw e;                                      // 400: our bug (e.g. a field not in the mapping)
        } catch (IOException e) {                         // no connection, or timed out
            throw new IndexUnavailableException(e);
        }
    }

    /** Each filter that was given becomes one clause. Tombstones never match. */
    private static BoolQuery.Builder filters(BoolQuery.Builder bool, ProductSearchCriteria criteria) {
        bool.filter(f -> f.term(t -> t.field("deleted").value(false)));
        String text = blankToNull(criteria.text());
        if (text != null) {
            bool.must(m -> m.multiMatch(mm -> mm
                    .query(text)
                    .fields("name^3", "description")      // a name match counts three times
                    .fuzziness("AUTO")));                 // 1 typo in 3-5 letters, 2 in 6 or more
        }
        String category = blankToNull(criteria.category());
        if (category != null) {
            bool.filter(f -> f.term(t -> t.field("category").value(category.toUpperCase(Locale.ROOT))));
        }
        if (criteria.minPrice() != null || criteria.maxPrice() != null) {
            bool.filter(f -> f.range(r -> r.number(n -> {
                n.field("price");
                if (criteria.minPrice() != null) {
                    n.gte(criteria.minPrice().doubleValue());
                }
                if (criteria.maxPrice() != null) {
                    n.lte(criteria.maxPrice().doubleValue());
                }
                return n;
            })));
        }
        return bool;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}