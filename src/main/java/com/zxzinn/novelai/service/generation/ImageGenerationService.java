package com.zxzinn.novelai.service.generation;

import com.google.inject.Inject;
import com.zxzinn.novelai.api.GenerationPayload;
import com.zxzinn.novelai.api.APIClient;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Log4j2
public class ImageGenerationService {
    private final APIClient apiClient;

    @Inject
    public ImageGenerationService(APIClient apiClient) {
        this.apiClient = apiClient;
    }

    public byte[] generateImage(GenerationPayload payload, String apiKey) throws IOException {
        int maxRetries = 3;
        int retryDelay = 5000;

        for (int i = 0; i < maxRetries; i++) {
            try {
                byte[] zipData = apiClient.generateImage(payload, apiKey);
                return extractImageFromZip(zipData);
            } catch (IOException e) {
                if (i == maxRetries - 1) {
                    throw e;
                }
                log.warn("生成圖像失敗,將在{}毫秒後重試. 錯誤: {}", retryDelay, e.getMessage());
                try {
                    Thread.sleep(retryDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("生成圖像被中斷", ie);
                }
            }
        }
        throw new IOException("超過最大重試次數");
    }

    @NotNull
    private byte[] extractImageFromZip(byte[] zipData) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData))) {
            ZipEntry entry = zis.getNextEntry();
            if (entry == null) {
                throw new IOException("ZIP文件為空");
            }
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = zis.read(buffer)) > 0) {
                byteArrayOutputStream.write(buffer, 0, len);
            }
            return byteArrayOutputStream.toByteArray();
        }
    }
}