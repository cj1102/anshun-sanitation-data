package com.anshun.dms.storage;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UploadFileInspectorTest {
    private static final long MAX_SIZE = 20L * 1024 * 1024;
    private final UploadFileInspector inspector = new UploadFileInspector();

    @Test
    void rejectsSpoofedPdfEvenWhenClientContentTypeClaimsPdf() {
        MockMultipartFile file = new MockMultipartFile("file", "contract.pdf", "application/pdf",
                "not a pdf".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> inspector.inspectAttachment(file, MAX_SIZE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("附件扩展名与实际文件内容不一致");
    }

    @Test
    void derivesImageTypeFromSignatureInsteadOfClientHeader() {
        byte[] png = new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2};
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "application/octet-stream", png);

        assertThat(inspector.inspectAttachment(file, MAX_SIZE).contentType()).isEqualTo("image/png");
    }

    @Test
    void acceptsUtf8MarkdownAndDerivesTrustedType() {
        MockMultipartFile file = new MockMultipartFile("file", "guide.md", "application/octet-stream",
                "# 合同管理\n仅限授权角色查看。".getBytes(StandardCharsets.UTF_8));

        assertThat(inspector.inspectKnowledge(file, MAX_SIZE).contentType()).isEqualTo("text/markdown");
    }

    @Test
    void rejectsMalformedUtf8KnowledgeText() {
        MockMultipartFile file = new MockMultipartFile("file", "guide.txt", "text/plain",
                new byte[]{(byte) 0xC3, 0x28});

        assertThatThrownBy(() -> inspector.inspectKnowledge(file, MAX_SIZE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("知识库文本必须使用 UTF-8 编码");
    }
}
