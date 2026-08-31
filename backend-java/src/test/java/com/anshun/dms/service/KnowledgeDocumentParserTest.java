package com.anshun.dms.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeDocumentParserTest {
    private final KnowledgeDocumentParser parser = new KnowledgeDocumentParser();

    @Test
    void parsesTextFileIntoRetrievableChunks() {
        MockMultipartFile file = new MockMultipartFile("file", "operation-guide.txt", "text/plain",
                "合同附件需要上传到 MinIO，只有拥有合同查看权限的用户可以下载。".getBytes(StandardCharsets.UTF_8));

        List<KnowledgeDocumentParser.ParsedChunk> chunks = parser.parse(file);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).text()).contains("MinIO").contains("合同查看权限");
        assertThat(chunks.get(0).pageStart()).isNull();
    }
}
