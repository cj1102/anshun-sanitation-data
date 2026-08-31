package com.anshun.dms.service;

import com.anshun.dms.common.BusinessException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Extracts text with page provenance; scanned PDFs need OCR before being uploaded. */
@Service
public class KnowledgeDocumentParser {
    private static final int CHUNK_SIZE = 800;
    private static final int CHUNK_OVERLAP = 120;
    private static final int MAX_CHUNKS = 500;
    private static final int MAX_PDF_PAGES = 200;

    public List<ParsedChunk> parse(MultipartFile file) {
        String extension = extensionOf(file);
        try {
            List<ParsedChunk> chunks = "pdf".equals(extension) ? parsePdf(file) : chunk(clean(new String(file.getBytes(), StandardCharsets.UTF_8)), null);
            if (chunks.isEmpty()) throw BusinessException.badRequest("未从文件中提取到可检索文本；扫描版 PDF 请先进行 OCR");
            if (chunks.size() > MAX_CHUNKS) throw BusinessException.badRequest("文档文本过长，请拆分后再上传");
            return chunks;
        } catch (IOException exception) {
            throw BusinessException.badRequest("文件解析失败，请确认 PDF 或文本文件未损坏");
        }
    }

    private List<ParsedChunk> parsePdf(MultipartFile file) throws IOException {
        List<ParsedChunk> result = new ArrayList<>();
        try (PDDocument document = PDDocument.load(file.getInputStream(), MemoryUsageSetting.setupMixed(16L * 1024 * 1024))) {
            if (document.getNumberOfPages() > MAX_PDF_PAGES) {
                throw BusinessException.badRequest("PDF 页数不能超过 " + MAX_PDF_PAGES + " 页，请拆分后再上传");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                result.addAll(chunk(clean(stripper.getText(document)), page));
            }
        }
        return result;
    }

    private List<ParsedChunk> chunk(String text, Integer page) {
        if (!StringUtils.hasText(text)) return List.of();
        List<ParsedChunk> result = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + CHUNK_SIZE);
            if (end < text.length()) {
                int split = Math.max(text.lastIndexOf('\n', end), Math.max(text.lastIndexOf('。', end), text.lastIndexOf('！', end)));
                if (split > start + CHUNK_SIZE / 2) end = split + 1;
            }
            String value = text.substring(start, end).trim();
            if (!value.isBlank()) result.add(new ParsedChunk(page, page, value));
            if (end >= text.length()) break;
            start = Math.max(end - CHUNK_OVERLAP, start + 1);
        }
        return result;
    }

    private String clean(String text) { return text == null ? "" : text.replace('\u0000', ' ').replaceAll("[ \\t]+", " ").replaceAll("\\n{3,}", "\\n\\n").trim(); }
    private String extensionOf(MultipartFile file) {
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        return extension == null ? "" : extension.toLowerCase(Locale.ROOT);
    }

    public record ParsedChunk(Integer pageStart, Integer pageEnd, String text) { }
}
