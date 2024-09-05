package com.zxzinn.novelai.service.generation;

import com.zxzinn.novelai.api.APIClient;
import com.zxzinn.novelai.model.GenerationResult;
import com.zxzinn.novelai.model.GenerationTask;
import com.zxzinn.novelai.utils.strategy.ExponentialBackoffRetry;
import com.zxzinn.novelai.utils.strategy.RetryStrategy;
import lombok.extern.log4j.Log4j2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Log4j2
public class GenerationTaskManager {
    private static GenerationTaskManager instance;
    private final ExecutorService executorService;
    private final APIClient apiClient;
    private final RetryStrategy retryStrategy;

    private GenerationTaskManager() {
        this.executorService = Executors.newSingleThreadExecutor();
        this.apiClient = new APIClient();
        this.retryStrategy = new ExponentialBackoffRetry();
    }

    public static synchronized GenerationTaskManager getInstance() {
        if (instance == null) {
            instance = new GenerationTaskManager();
        }
        return instance;
    }

    public CompletableFuture<GenerationResult> submitTask(GenerationTask task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                byte[] zipData = retryStrategy.execute(() -> apiClient.generateImage(task.payload(), task.apiKey()))
                        .orElseThrow(() -> new RuntimeException("Image generation failed after retries"));
                byte[] imageData = extractImageFromZip(zipData);
                return GenerationResult.success(imageData);
            } catch (Exception e) {
                log.error("Error generating image: ", e);
                return GenerationResult.failure(e.getMessage());
            }
        }, executorService);
    }

    private byte[] extractImageFromZip(byte[] zipData) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData))) {
            ZipEntry entry = zis.getNextEntry();
            if (entry == null) {
                throw new IOException("ZIP file is empty");
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            zis.transferTo(outputStream);
            return outputStream.toByteArray();
        }
    }

    public void shutdown() {
        executorService.shutdown();
    }
}