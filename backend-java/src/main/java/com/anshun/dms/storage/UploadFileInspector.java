package com.anshun.dms.storage;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Derives a trusted content type from file bytes instead of trusting the multipart Content-Type header. */
@Component
public class UploadFileInspector {
    private static final Set<String> ATTACHMENT_EXTENSIONS = Set.of("pdf", "png", "jpg", "jpeg");
    private static final Set<String> KNOWLEDGE_EXTENSIONS = Set.of("pdf", "txt", "md");
    private static final Map<String, String> ATTACHMENT_TYPES = Map.of(
            "pdf", MediaType.APPLICATION_PDF_VALUE,
            "png", MediaType.IMAGE_PNG_VALUE,
            "jpg", MediaType.IMAGE_JPEG_VALUE,
            "jpeg", MediaType.IMAGE_JPEG_VALUE);

    public Inspection inspectAttachment(MultipartFile file, long maxFileSize) {
        String extension = requireFile(file, maxFileSize, ATTACHMENT_EXTENSIONS,
                "请选择需要上传的附件", "附件大小不能超过 20MB", "仅支持 PDF、PNG、JPG 格式的附件");
        byte[] signature = signature(file);
        boolean matches = switch (extension) {
            case "pdf" -> isPdf(signature);
            case "png" -> isPng(signature);
            case "jpg", "jpeg" -> isJpeg(signature);
            default -> false;
        };
        if (!matches) throw new IllegalArgumentException("附件扩展名与实际文件内容不一致");
        return new Inspection(ATTACHMENT_TYPES.get(extension));
    }

    public Inspection inspectKnowledge(MultipartFile file, long maxFileSize) {
        String extension = requireFile(file, maxFileSize, KNOWLEDGE_EXTENSIONS,
                "请选择需要上传的知识库文件", "知识库文件大小不能超过 20MB", "知识库仅支持 PDF、TXT、Markdown 文件");
        if ("pdf".equals(extension)) {
            if (!isPdf(signature(file))) throw new IllegalArgumentException("PDF 扩展名与实际文件内容不一致");
            return new Inspection(MediaType.APPLICATION_PDF_VALUE);
        }
        validateUtf8Text(file);
        return new Inspection("md".equals(extension) ? "text/markdown" : MediaType.TEXT_PLAIN_VALUE);
    }

    private String requireFile(MultipartFile file, long maxFileSize, Set<String> allowedExtensions,
                               String emptyMessage, String sizeMessage, String typeMessage) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException(emptyMessage);
        if (file.getSize() > maxFileSize) throw new IllegalArgumentException(sizeMessage);
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (extension == null || !allowedExtensions.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException(typeMessage);
        }
        return extension.toLowerCase(Locale.ROOT);
    }

    private byte[] signature(MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            return input.readNBytes(16);
        } catch (IOException exception) {
            throw new IllegalArgumentException("无法读取上传文件，请重新选择文件");
        }
    }

    private void validateUtf8Text(MultipartFile file) {
        try (InputStream input = file.getInputStream();
             Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8.newDecoder()
                     .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT))) {
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) >= 0) {
                for (int index = 0; index < read; index++) {
                    char value = buffer[index];
                    if (value == '\u0000' || (value < 0x20 && value != '\n' && value != '\r' && value != '\t')) {
                        throw new IllegalArgumentException("知识库文本包含非法控制字符");
                    }
                }
            }
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("知识库文本必须使用 UTF-8 编码");
        } catch (IOException exception) {
            throw new IllegalArgumentException("无法读取知识库文件，请重新选择文件");
        }
    }

    private boolean isPdf(byte[] value) {
        return startsWith(value, new byte[]{'%', 'P', 'D', 'F', '-'});
    }

    private boolean isPng(byte[] value) {
        return startsWith(value, new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
    }

    private boolean isJpeg(byte[] value) {
        return value.length >= 3 && value[0] == (byte) 0xFF && value[1] == (byte) 0xD8 && value[2] == (byte) 0xFF;
    }

    private boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) return false;
        for (int index = 0; index < prefix.length; index++) if (value[index] != prefix[index]) return false;
        return true;
    }

    public record Inspection(String contentType) { }
}
