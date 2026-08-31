package com.anshun.dms.service;

import com.anshun.dms.ai.vector.KnowledgeEmbeddingService;
import com.anshun.dms.ai.vector.KnowledgeVectorStore;
import com.anshun.dms.common.BusinessException;
import com.anshun.dms.common.TransactionCallbacks;
import com.anshun.dms.mapper.AiKnowledgeMapper;
import com.anshun.dms.storage.MinioStorageService;
import com.anshun.dms.storage.StorageCleanupService;
import com.anshun.dms.vo.AiKnowledgeDocumentVO;
import com.anshun.dms.vo.AiKnowledgeSourceVO;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hybrid RAG retrieval. MySQL remains the source of truth for role filtering and document text;
 * Qdrant is an optional acceleration layer for vector recall and never becomes an authorization boundary.
 */
@Service
public class AiKnowledgeService {
    private static final Logger log = LoggerFactory.getLogger(AiKnowledgeService.class);
    private static final Set<String> ROLE_CODES = Set.of("ADMIN", "OPERATOR", "FINANCE", "AUDITOR", "VIEWER");
    private final AiKnowledgeMapper mapper;
    private final KnowledgeDocumentParser parser;
    private final MinioStorageService storage;
    private final StorageCleanupService storageCleanup;
    private final KnowledgeEmbeddingService embeddingService;
    private final KnowledgeVectorStore vectorStore;

    public AiKnowledgeService(AiKnowledgeMapper mapper, KnowledgeDocumentParser parser, MinioStorageService storage,
                              StorageCleanupService storageCleanup, KnowledgeEmbeddingService embeddingService,
                              KnowledgeVectorStore vectorStore) {
        this.mapper = mapper;
        this.parser = parser;
        this.storage = storage;
        this.storageCleanup = storageCleanup;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
    }

    @Transactional
    public AiKnowledgeDocumentVO upload(MultipartFile file, String title, String visibleRoles, String username) {
        storage.validateKnowledge(file);
        List<KnowledgeDocumentParser.ParsedChunk> parsedChunks = parser.parse(file);
        Integer userId = mapper.selectUserId(username);
        if (userId == null) throw BusinessException.notFound("当前用户不存在或已禁用");
        String roles = normalizeRoles(visibleRoles);
        MinioStorageService.StoredObject stored = storage.uploadKnowledge(file);
        TransactionCallbacks.afterRollback(() -> enqueueCleanupQuietly(stored.objectName()));
        try {
            String documentTitle = StringUtils.hasText(title) ? trim(title, 150) : titleFromFilename(stored.originalFilename());
            AiKnowledgeMapper.KnowledgeDocumentDraft draft = new AiKnowledgeMapper.KnowledgeDocumentDraft(documentTitle,
                    stored.originalFilename(), stored.objectName(), stored.contentType(), stored.size(), roles, userId, username);
            mapper.insertDocument(draft);
            if (draft.getDocumentId() == null) throw BusinessException.unavailable("知识库文档保存失败，请稍后重试");
            List<AiKnowledgeMapper.KnowledgeChunkDraft> chunks = new ArrayList<>();
            for (int index = 0; index < parsedChunks.size(); index++) {
                KnowledgeDocumentParser.ParsedChunk chunk = parsedChunks.get(index);
                chunks.add(new AiKnowledgeMapper.KnowledgeChunkDraft(draft.getDocumentId(), index + 1, chunk.pageStart(), chunk.pageEnd(), chunk.text()));
            }
            mapper.insertChunks(chunks);
            TransactionCallbacks.afterCommit(() -> indexDocumentQuietly(draft.getDocumentId()));
            return mapper.selectDocuments().stream().filter(item -> item.documentId().equals(draft.getDocumentId())).findFirst()
                    .orElseThrow(() -> BusinessException.unavailable("知识库文档保存失败，请稍后重试"));
        } catch (RuntimeException exception) {
            deleteUploadedObjectQuietly(stored.objectName());
            throw exception;
        }
    }

    public List<AiKnowledgeDocumentVO> listDocuments() { return mapper.selectDocuments(); }

    @Transactional
    public void delete(long documentId) {
        AiKnowledgeMapper.KnowledgeDocumentRecord document = mapper.selectDocument(documentId);
        if (document == null) throw BusinessException.notFound("知识库文档不存在");
        if (mapper.logicalDelete(documentId) != 1) throw BusinessException.conflict("知识库文档状态已发生变化，请刷新后重试");
        storageCleanup.enqueue(document.objectName());
        TransactionCallbacks.afterCommit(() -> deleteVectorDocumentQuietly(documentId));
    }

    /** Rebuilds the optional vector index from MySQL, allowing safe recovery after Qdrant maintenance. */
    public ReindexResult reindexVectors() {
        if (!vectorStore.enabled()) return new ReindexResult(0, 0, false, embeddingService.model());
        vectorStore.initialize();
        List<AiKnowledgeMapper.KnowledgeChunkCandidate> chunks = mapper.selectAllChunks();
        indexChunks(chunks);
        long documents = chunks.stream().map(AiKnowledgeMapper.KnowledgeChunkCandidate::documentId).distinct().count();
        return new ReindexResult(documents, chunks.size(), true, embeddingService.model());
    }

    public RetrievalResult retrieve(String question, Authentication authentication) {
        if (!StringUtils.hasText(question)) return RetrievalResult.empty();
        List<String> roles = authentication == null ? List.of() : authentication.getAuthorities().stream()
                .map(item -> item.getAuthority()).filter(value -> value.startsWith("ROLE_")).map(value -> value.substring(5)).toList();
        Set<String> terms = queryTerms(question);
        Map<Long, Double> vectorScores = vectorScores(question);
        List<AiKnowledgeMapper.KnowledgeChunkCandidate> lexicalCandidates = terms.isEmpty()
                ? List.of() : mapper.selectAccessibleChunks(roles);
        List<AiKnowledgeMapper.KnowledgeChunkCandidate> vectorCandidates = vectorScores.isEmpty()
                ? List.of() : mapper.selectAccessibleChunksByIds(new ArrayList<>(vectorScores.keySet()), roles);

        // Qdrant only supplies untrusted IDs and scores. MySQL hydrates every vector hit under the
        // same READY/deleted/visible_roles predicate as lexical retrieval, then both channels merge.
        Map<Long, AiKnowledgeMapper.KnowledgeChunkCandidate> candidates = new LinkedHashMap<>();
        for (AiKnowledgeMapper.KnowledgeChunkCandidate chunk : lexicalCandidates) candidates.put(chunk.chunkId(), chunk);
        for (AiKnowledgeMapper.KnowledgeChunkCandidate chunk : vectorCandidates) candidates.put(chunk.chunkId(), chunk);
        List<ScoredChunk> scored = candidates.values().stream()
                .map(chunk -> scoreHybrid(chunk, terms, vectorScores.get(chunk.chunkId())))
                .filter(item -> item.hybridScore() > 0)
                .sorted(Comparator.comparingDouble(ScoredChunk::hybridScore).reversed())
                .limit(4)
                .toList();
        if (scored.isEmpty()) return RetrievalResult.empty();
        List<AiKnowledgeSourceVO> sources = new ArrayList<>();
        StringBuilder context = new StringBuilder();
        for (int index = 0; index < scored.size(); index++) {
            AiKnowledgeMapper.KnowledgeChunkCandidate chunk = scored.get(index).chunk();
            sources.add(new AiKnowledgeSourceVO(chunk.documentId(), chunk.title(), chunk.pageStart(), chunk.pageEnd(), excerpt(chunk.chunkText())));
            context.append("[资料").append(index + 1).append("] ").append(chunk.title());
            if (chunk.pageStart() != null) context.append("（第 ").append(chunk.pageStart()).append(" 页）");
            context.append("\n").append(chunk.chunkText()).append("\n\n");
        }
        return new RetrievalResult(List.copyOf(sources), context.toString().trim());
    }

    public String appendSourceList(String answer, List<AiKnowledgeSourceVO> sources) {
        if (sources.isEmpty()) return answer;
        StringBuilder result = new StringBuilder(answer).append("\n\n参考资料：\n");
        for (int index = 0; index < sources.size(); index++) {
            AiKnowledgeSourceVO source = sources.get(index);
            result.append("- [资料").append(index + 1).append("] ").append(source.title());
            if (source.pageStart() != null) result.append("（第 ").append(source.pageStart()).append(" 页）");
            result.append('\n');
        }
        return result.toString().trim();
    }

    private void indexDocumentQuietly(long documentId) {
        if (!vectorStore.enabled()) return;
        try {
            indexChunks(mapper.selectChunksByDocument(documentId));
        } catch (RuntimeException exception) {
            // Uploads remain usable through lexical RAG if the optional vector infrastructure is down.
            log.warn("Knowledge vector indexing deferred: documentId={}, errorType={}", documentId, exception.getClass().getSimpleName());
        }
    }

    private void deleteVectorDocumentQuietly(long documentId) {
        try {
            vectorStore.deleteDocument(documentId);
        } catch (RuntimeException exception) {
            // MySQL authorization filtering hides stale vector points; a later rebuild will remove them.
            log.warn("Knowledge vector cleanup deferred: documentId={}, errorType={}", documentId,
                    exception.getClass().getSimpleName());
        }
    }

    private void deleteUploadedObjectQuietly(String objectName) {
        try {
            storage.delete(objectName);
        } catch (RuntimeException exception) {
            log.error("Knowledge upload compensation failed: objectName={}, errorType={}", objectName,
                    exception.getClass().getSimpleName());
        }
    }

    private void enqueueCleanupQuietly(String objectName) {
        try {
            storageCleanup.enqueue(objectName);
        } catch (RuntimeException exception) {
            log.error("Unable to enqueue rolled-back knowledge cleanup: objectName={}, errorType={}", objectName,
                    exception.getClass().getSimpleName());
        }
    }

    private void indexChunks(List<AiKnowledgeMapper.KnowledgeChunkCandidate> chunks) {
        List<KnowledgeVectorStore.VectorPoint> batch = new ArrayList<>(32);
        for (AiKnowledgeMapper.KnowledgeChunkCandidate chunk : chunks) {
            batch.add(new KnowledgeVectorStore.VectorPoint(vectorId(chunk), chunk.documentId(), chunk.chunkNo(),
                    embeddingService.embed(chunk.chunkText())));
            if (batch.size() == 32) {
                vectorStore.upsert(batch);
                batch = new ArrayList<>(32);
            }
        }
        if (!batch.isEmpty()) vectorStore.upsert(batch);
    }

    private Map<Long, Double> vectorScores(String question) {
        if (!vectorStore.enabled()) return Map.of();
        try {
            Map<Long, Double> result = new LinkedHashMap<>();
            for (KnowledgeVectorStore.VectorHit hit : vectorStore.search(embeddingService.embed(question), 24)) {
                result.merge(hit.id(), hit.score(), Math::max);
            }
            return result;
        } catch (RuntimeException exception) {
            log.warn("Knowledge vector retrieval fallback: errorType={}", exception.getClass().getSimpleName());
            return Map.of();
        }
    }

    private ScoredChunk scoreHybrid(AiKnowledgeMapper.KnowledgeChunkCandidate chunk, Set<String> terms, Double vectorScore) {
        int lexicalScore = score(chunk.chunkText(), terms);
        // Cosine scores are generally in [-1, 1]. A document still needs lexical evidence or a positive vector hit.
        double semantic = vectorScore == null ? 0 : Math.max(0, Math.min(1, vectorScore));
        double lexical = Math.min(1D, lexicalScore / 8D);
        double hybrid = vectorScore == null ? lexical : (semantic * 0.60D + lexical * 0.40D);
        return new ScoredChunk(chunk, lexicalScore, hybrid);
    }

    private long vectorId(AiKnowledgeMapper.KnowledgeChunkCandidate chunk) {
        if (chunk.chunkId() == null) throw new IllegalStateException("知识分块缺少数据库主键，无法建立向量索引");
        return chunk.chunkId();
    }

    private int score(String content, Set<String> terms) {
        String normalized = normalize(content);
        int score = 0;
        for (String term : terms) {
            int position = normalized.indexOf(term);
            while (position >= 0) { score += term.length() >= 3 ? 3 : 1; position = normalized.indexOf(term, position + term.length()); }
        }
        return score;
    }

    private Set<String> queryTerms(String question) {
        String value = normalize(question);
        Set<String> terms = new LinkedHashSet<>();
        for (int index = 0; index < value.length() - 1; index++) {
            char first = value.charAt(index), second = value.charAt(index + 1);
            if (Character.isLetterOrDigit(first) && Character.isLetterOrDigit(second)) terms.add(value.substring(index, index + 2));
        }
        for (String token : value.split("[^a-z0-9\\p{IsHan}]+")) if (token.length() >= 3) terms.add(token);
        return terms;
    }

    private String normalize(String value) { return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "").replaceAll("[^a-z0-9\\p{IsHan}]", ""); }
    private String normalizeRoles(String value) {
        if (!StringUtils.hasText(value) || "ALL".equalsIgnoreCase(value.trim())) return "ALL";
        Set<String> roles = new LinkedHashSet<>();
        for (String role : value.split(",")) {
            String normalized = role.trim().toUpperCase(Locale.ROOT);
            if (!ROLE_CODES.contains(normalized)) throw BusinessException.badRequest("文档可见角色不合法");
            roles.add(normalized);
        }
        return roles.isEmpty() ? "ALL" : String.join(",", roles);
    }
    private String titleFromFilename(String filename) {
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        return trim(dot > 0 ? filename.substring(0, dot) : filename == null ? "未命名文档" : filename, 150);
    }
    private String trim(String value, int max) { return value.length() <= max ? value : value.substring(0, max); }
    private String excerpt(String value) { String normalized = value.replaceAll("\\s+", " ").trim(); return normalized.length() <= 160 ? normalized : normalized.substring(0, 160) + "…"; }

    private record ScoredChunk(AiKnowledgeMapper.KnowledgeChunkCandidate chunk, int lexicalScore, double hybridScore) { }
    public record RetrievalResult(List<AiKnowledgeSourceVO> sources, String context) {
        static RetrievalResult empty() { return new RetrievalResult(List.of(), ""); }
        public boolean hasSources() { return !sources.isEmpty(); }
    }
    public record ReindexResult(long documentCount, long chunkCount, boolean vectorStoreEnabled, String embeddingModel) { }
}
