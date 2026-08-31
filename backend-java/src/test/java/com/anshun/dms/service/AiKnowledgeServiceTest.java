package com.anshun.dms.service;

import com.anshun.dms.ai.vector.KnowledgeEmbeddingService;
import com.anshun.dms.ai.vector.KnowledgeVectorStore;
import com.anshun.dms.mapper.AiKnowledgeMapper;
import com.anshun.dms.storage.MinioStorageService;
import com.anshun.dms.storage.StorageCleanupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiKnowledgeServiceTest {
    @Mock AiKnowledgeMapper mapper;
    @Mock KnowledgeDocumentParser parser;
    @Mock MinioStorageService storage;
    @Mock StorageCleanupService storageCleanup;
    @Mock KnowledgeEmbeddingService embeddingService;
    @Mock KnowledgeVectorStore vectorStore;
    @InjectMocks AiKnowledgeService knowledgeService;

    @Test
    void hydratesOldVectorHitByChunkIdEvenWhenAbsentFromRecentLexicalPool() {
        String question = "合同逾期应该怎样处理";
        AiKnowledgeMapper.KnowledgeChunkCandidate oldChunk = chunk(7L, 2L, "历史合同制度", "承租单位逾期时应启动催缴流程");
        when(vectorStore.enabled()).thenReturn(true);
        when(embeddingService.embed(question)).thenReturn(List.of(0.2F, 0.8F));
        when(vectorStore.search(List.of(0.2F, 0.8F), 24))
                .thenReturn(List.of(new KnowledgeVectorStore.VectorHit(7L, 0.96D)));
        when(mapper.selectAccessibleChunks(List.of("VIEWER"))).thenReturn(List.of());
        when(mapper.selectAccessibleChunksByIds(List.of(7L), List.of("VIEWER"))).thenReturn(List.of(oldChunk));

        AiKnowledgeService.RetrievalResult result = knowledgeService.retrieve(question, viewer());

        assertThat(result.hasSources()).isTrue();
        assertThat(result.sources()).extracting(source -> source.documentId()).containsExactly(2L);
        assertThat(result.context()).contains("历史合同制度", "催缴流程");
        verify(mapper).selectAccessibleChunksByIds(List.of(7L), List.of("VIEWER"));
    }

    @Test
    void neverTrustsVectorHitWithoutMysqlAclApproval() {
        String question = "查看财务内部制度";
        when(vectorStore.enabled()).thenReturn(true);
        when(embeddingService.embed(question)).thenReturn(List.of(1F, 0F));
        when(vectorStore.search(List.of(1F, 0F), 24))
                .thenReturn(List.of(new KnowledgeVectorStore.VectorHit(99L, 0.99D)));
        when(mapper.selectAccessibleChunks(List.of("VIEWER"))).thenReturn(List.of());
        when(mapper.selectAccessibleChunksByIds(List.of(99L), List.of("VIEWER"))).thenReturn(List.of());

        AiKnowledgeService.RetrievalResult result = knowledgeService.retrieve(question, viewer());

        assertThat(result.hasSources()).isFalse();
        assertThat(result.context()).isEmpty();
    }

    @Test
    void fallsBackToLexicalRetrievalWhenVectorStoreFails() {
        String question = "点位归档流程";
        AiKnowledgeMapper.KnowledgeChunkCandidate lexicalChunk = chunk(1200L, 8L, "点位操作手册", "点位归档流程需要管理员复核");
        when(vectorStore.enabled()).thenReturn(true);
        when(embeddingService.embed(question)).thenThrow(new IllegalStateException("embedding unavailable"));
        when(mapper.selectAccessibleChunks(List.of("VIEWER"))).thenReturn(List.of(lexicalChunk));

        AiKnowledgeService.RetrievalResult result = knowledgeService.retrieve(question, viewer());

        assertThat(result.hasSources()).isTrue();
        assertThat(result.context()).contains("点位操作手册", "管理员复核");
        verify(mapper, never()).selectAccessibleChunksByIds(org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void indexesVectorsWithTheMysqlChunkPrimaryKey() {
        AiKnowledgeMapper.KnowledgeChunkCandidate chunk = chunk(4321L, 8L, "点位操作手册", "点位归档流程");
        when(vectorStore.enabled()).thenReturn(true);
        when(mapper.selectAllChunks()).thenReturn(List.of(chunk));
        when(embeddingService.embed("点位归档流程")).thenReturn(List.of(0.4F, 0.6F));

        knowledgeService.reindexVectors();

        verify(vectorStore).initialize();
        verify(vectorStore).upsert(argThat(points -> points.size() == 1
                && points.get(0).id() == 4321L
                && points.get(0).documentId() == 8L));
    }

    @Test
    void deletesMetadataBeforeEnqueuingObjectCleanup() {
        when(mapper.selectDocument(12L)).thenReturn(
                new AiKnowledgeMapper.KnowledgeDocumentRecord(12L, "制度", "knowledge/file.pdf", "ALL"));
        when(mapper.logicalDelete(12L)).thenReturn(1);

        knowledgeService.delete(12L);

        verify(mapper).logicalDelete(12L);
        verify(storageCleanup).enqueue("knowledge/file.pdf");
        verify(storage, never()).delete("knowledge/file.pdf");
        verify(vectorStore).deleteDocument(12L);
    }

    private Authentication viewer() {
        return new UsernamePasswordAuthenticationToken("viewer", "",
                List.of(new SimpleGrantedAuthority("ROLE_VIEWER")));
    }

    private AiKnowledgeMapper.KnowledgeChunkCandidate chunk(long chunkId, long documentId, String title, String text) {
        return new AiKnowledgeMapper.KnowledgeChunkCandidate(chunkId, documentId, title, 1, 3, 3, text);
    }
}
