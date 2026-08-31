package com.anshun.dms.ai.vector;

import java.util.List;

/**
 * Storage boundary for knowledge vectors. Business retrieval does not depend on a specific vector database,
 * so Qdrant can later be replaced by LanceDB, Milvus or a managed provider without changing RAG callers.
 */
public interface KnowledgeVectorStore {
    boolean enabled();

    /** Creates or checks the backing collection without writing document content. */
    default void initialize() { }

    void upsert(List<VectorPoint> points);
    List<VectorHit> search(List<Float> vector, int limit);
    void deleteDocument(long documentId);

    record VectorPoint(long id, long documentId, int chunkNo, List<Float> vector) { }
    record VectorHit(long id, double score) { }
}
