package com.anshun.dms.ai.vector;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeEmbeddingServiceTest {
    @Test
    void localEmbeddingIsDeterministicNormalizedAndDimensionSafe() {
        KnowledgeEmbeddingService service = new KnowledgeEmbeddingService("hash", "", "", "local-hash-ngram-v1", 256);

        List<Float> first = service.embed("合同审核需要填写审核意见");
        List<Float> second = service.embed("合同审核需要填写审核意见");
        double squaredLength = first.stream().mapToDouble(value -> value * value).sum();

        assertThat(first).hasSize(256).isEqualTo(second);
        assertThat(squaredLength).isCloseTo(1D, org.assertj.core.data.Offset.offset(0.0001D));
        assertThat(service.semanticProvider()).isFalse();
    }
}
