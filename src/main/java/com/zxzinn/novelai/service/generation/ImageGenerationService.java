package com.zxzinn.novelai.service.generation;

import com.google.inject.Inject;
import com.zxzinn.novelai.api.GenerationPayload;
import com.zxzinn.novelai.api.APIClient;
import lombok.extern.log4j.Log4j2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Log4j2
public class ImageGenerationService {
    private final APIClient apiClient;

    @Inject
    public ImageGenerationService(APIClient apiClient) {
        this.apiClient = apiClient;
    }

    public Optional<byte[]> generateImage(GenerationPayload payload, String apiKey) {
        try {
            byte[] zipData = apiClient.generateImage(payload, apiKey);
            return Optional.of(extractImageFromZip(zipData));
        } catch (IOException e) {
            log.error("生成圖像時發生錯誤：{}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    private byte[] extractImageFromZip(byte[] zipData) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData))) {
            ZipEntry entry = zis.getNextEntry();
            if (entry == null) {
                throw new IOException("ZIP文件為空");
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            zis.transferTo(outputStream);
            return outputStream.toByteArray();
        }
    }
}