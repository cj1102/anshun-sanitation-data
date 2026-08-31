package com.anshun.dms.ai.vector;

import com.anshun.dms.common.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.time.Duration;

/**
 * Supports a real OpenAI-compatible embedding endpoint when configured. The deterministic n-gram
 * provider is intentionally the safe local default: it enables offline demos without sending documents
 * to another provider, while deployments can switch to a semantic embedding model by configuration.
 */
@Service
public class KnowledgeEmbeddingService {
    private final String provider;
    private final String apiKey;
    private final String model;
    private final int dimensions;
    private final RestClient client;

    public KnowledgeEmbeddingService(@Value("${app.ai.embedding.provider:hash}") String provider,
                                     @Value("${app.ai.embedding.base-url:}") String baseUrl,
                                     @Value("${app.ai.embedding.api-key:}") String apiKey,
                                     @Value("${app.ai.embedding.model:}") String model,
                                     @Value("${app.ai.embedding.dimensions:256}") int dimensions) {
        this.provider = provider == null ? "hash" : provider.trim().toLowerCase(Locale.ROOT);
        this.apiKey = apiKey;
        this.model = StringUtils.hasText(model) ? model : "local-hash-ngram-v1";
        if (dimensions < 32 || dimensions > 4096) throw new IllegalArgumentException("Embedding 维度必须在 32 到 4096 之间");
        this.dimensions = dimensions;
        this.client = "openai".equals(this.provider) && StringUtils.hasText(baseUrl)
                ? remoteClient(baseUrl) : null;
    }

    public List<Float> embed(String text) {
        if (!StringUtils.hasText(text)) throw BusinessException.badRequest("无法为无内容的知识库分段生成向量");
        return "openai".equals(provider) ? remoteEmbedding(text) : hashEmbedding(text);
    }

    public String model() { return model; }
    public int dimensions() { return dimensions; }
    public boolean semanticProvider() { return "openai".equals(provider); }

    private RestClient remoteClient(String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(30));
        return RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    private List<Float> remoteEmbedding(String text) {
        if (client == null || !StringUtils.hasText(apiKey)) {
            throw BusinessException.unavailable("已启用远程 Embedding，但尚未配置 AI_EMBEDDING_BASE_URL 或 AI_EMBEDDING_API_KEY");
        }
        try {
            JsonNode response = client.post().uri("/embeddings")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(java.util.Map.of("model", model, "input", text))
                    .retrieve().body(JsonNode.class);
            JsonNode values = response == null ? null : response.path("data").path(0).path("embedding");
            if (values == null || !values.isArray() || values.size() != dimensions) {
                throw BusinessException.unavailable("Embedding 服务返回的向量维度与 AI_EMBEDDING_DIMENSIONS 不一致");
            }
            List<Float> vector = new ArrayList<>(dimensions);
            for (JsonNode value : values) vector.add((float) value.asDouble());
            return normalize(vector);
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw BusinessException.unavailable("Embedding 服务暂时不可用，请稍后重试");
        }
    }

    private List<Float> hashEmbedding(String source) {
        float[] values = new float[dimensions];
        String normalized = source.toLowerCase(Locale.ROOT).replaceAll("\\s+", "").trim();
        // Character bigrams are robust for Chinese and preserve a useful local/offline retrieval fallback.
        for (int index = 0; index < normalized.length(); index++) {
            String token = normalized.substring(index, Math.min(normalized.length(), index + 2));
            int hash = fnv1a(token);
            int slot = Math.floorMod(hash, dimensions);
            values[slot] += (hash & 1) == 0 ? 1F : -1F;
        }
        List<Float> vector = new ArrayList<>(dimensions);
        for (float value : values) vector.add(value);
        return normalize(vector);
    }

    private int fnv1a(String value) {
        int hash = 0x811c9dc5;
        for (int index = 0; index < value.length(); index++) {
            hash ^= value.charAt(index);
            hash *= 0x01000193;
        }
        return hash;
    }

    private List<Float> normalize(List<Float> vector) {
        double sum = 0;
        for (float value : vector) sum += value * value;
        if (sum == 0) return vector;
        float divisor = (float) Math.sqrt(sum);
        List<Float> normalized = new ArrayList<>(vector.size());
        for (float value : vector) normalized.add(value / divisor);
        return normalized;
    }
}
