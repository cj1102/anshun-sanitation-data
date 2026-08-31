package com.anshun.dms.ai.vector;

import com.anshun.dms.common.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.time.Duration;

/** Qdrant REST implementation. It stores only identifiers as payload, never raw knowledge text. */
@Service
public class QdrantKnowledgeVectorStore implements KnowledgeVectorStore {
    private static final Logger log = LoggerFactory.getLogger(QdrantKnowledgeVectorStore.class);
    /** Excludes points created by the former documentId/chunkNo-derived ID scheme. */
    private static final int INDEX_SCHEMA_VERSION = 2;
    private final boolean enabled;
    private final String collection;
    private final int dimensions;
    private final RestClient client;
    private final AtomicBoolean collectionReady = new AtomicBoolean(false);

    public QdrantKnowledgeVectorStore(@Value("${app.ai.vector-store.enabled:false}") boolean enabled,
                                      @Value("${app.ai.vector-store.base-url:http://127.0.0.1:6333}") String baseUrl,
                                      @Value("${app.ai.vector-store.collection:anshun_knowledge}") String collection,
                                      @Value("${app.ai.vector-store.dimensions:256}") int dimensions) {
        this.enabled = enabled;
        this.collection = collection;
        this.dimensions = dimensions;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(8));
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    @Override public boolean enabled() { return enabled; }

    @Override
    public void initialize() {
        if (enabled) ensureCollection();
    }

    @Override
    public void upsert(List<VectorPoint> points) {
        if (!enabled || points == null || points.isEmpty()) return;
        ensureCollection();
        List<Map<String, Object>> bodyPoints = new ArrayList<>();
        for (VectorPoint point : points) {
            if (point.vector().size() != dimensions) throw BusinessException.badRequest("知识库向量维度不匹配");
            bodyPoints.add(Map.of("id", point.id(), "vector", point.vector(),
                    "payload", Map.of("documentId", point.documentId(), "chunkNo", point.chunkNo(),
                            "schemaVersion", INDEX_SCHEMA_VERSION)));
        }
        client.put().uri("/collections/{collection}/points?wait=true", collection)
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("points", bodyPoints)).retrieve().toBodilessEntity();
    }

    @Override
    public List<VectorHit> search(List<Float> vector, int limit) {
        if (!enabled || vector == null || vector.isEmpty()) return List.of();
        ensureCollection();
        if (vector.size() != dimensions) throw BusinessException.badRequest("查询向量维度不匹配");
        Map<String, Object> versionFilter = Map.of("must", List.of(
                Map.of("key", "schemaVersion", "match", Map.of("value", INDEX_SCHEMA_VERSION))));
        JsonNode response = client.post().uri("/collections/{collection}/points/query", collection)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("query", vector, "filter", versionFilter,
                        "limit", Math.max(1, Math.min(limit, 50)), "with_payload", false))
                .retrieve().body(JsonNode.class);
        JsonNode result = response == null ? null : response.path("result");
        JsonNode points = result != null && result.has("points") ? result.path("points") : result;
        if (points == null || !points.isArray()) return List.of();
        List<VectorHit> hits = new ArrayList<>();
        for (JsonNode point : points) hits.add(new VectorHit(point.path("id").asLong(), point.path("score").asDouble()));
        return hits;
    }

    @Override
    public void deleteDocument(long documentId) {
        if (!enabled) return;
        ensureCollection();
        Map<String, Object> filter = Map.of("must", List.of(Map.of("key", "documentId", "match", Map.of("value", documentId))));
        client.post().uri("/collections/{collection}/points/delete?wait=true", collection)
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("filter", filter)).retrieve().toBodilessEntity();
    }

    private void ensureCollection() {
        if (collectionReady.get()) return;
        synchronized (collectionReady) {
            if (collectionReady.get()) return;
            try {
                client.put().uri("/collections/{collection}", collection).contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("vectors", Map.of("size", dimensions, "distance", "Cosine"))).retrieve().toBodilessEntity();
            } catch (RestClientResponseException exception) {
                if (exception.getStatusCode() != HttpStatus.CONFLICT) throw exception;
            }
            collectionReady.set(true);
            log.info("Qdrant knowledge collection is ready: collection={}, dimensions={}", collection, dimensions);
        }
    }
}
