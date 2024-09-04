package com.zxzinn.novelai.service.generation;

import com.google.inject.Inject;
import com.zxzinn.novelai.api.GenerationPayload;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Log4j2
public class GenerationHandler {
    private static final int MAX_RETRIES = 5;
    private static final long RETRY_DELAY_MS = 20000;
    private static final String FILE_NAME_FORMAT = "NovelAI_FX_generated_%s.png";
    private static final DateTimeFormatter FILE_NAME_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final ImageGenerationService imageGenerationService;
    private final RetryStrategy retryStrategy;

    @Inject
    public GenerationHandler(ImageGenerationService imageGenerationService) {
        this.imageGenerationService = imageGenerationService;
        this.retryStrategy = new ExponentialBackoffRetry(MAX_RETRIES, RETRY_DELAY_MS);
    }

    public Optional<byte[]> generateImageWithRetry(GenerationPayload payload, String apiKey) {
        return retryStrategy.execute(() -> imageGenerationService.generateImage(payload, apiKey));
    }

    public Optional<File> saveImage(byte[] imageData, String outputDir) {
        try {
            Path outputPath = ensureOutputDirectoryExists(outputDir);
            File outputFile = createOutputFile(outputPath);
            writeImageDataToFile(imageData, outputFile);
            return Optional.of(outputFile);
        } catch (IOException e) {
            log.error("保存圖像時發生錯誤：{}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    private Path ensureOutputDirectoryExists(String outputDir) throws IOException {
        Path outputPath = Paths.get(outputDir);
        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath);
        }
        return outputPath;
    }

    private File createOutputFile(Path outputPath) {
        String fileName = generateFileName();
        return outputPath.resolve(fileName).toFile();
    }

    private String generateFileName() {
        String timeStamp = LocalDateTime.now().format(FILE_NAME_TIMESTAMP_FORMATTER);
        return String.format(FILE_NAME_FORMAT, timeStamp);
    }

    private void writeImageDataToFile(byte[] imageData, File outputFile) throws IOException {
        Files.write(outputFile.toPath(), imageData);
    }

    private interface RetryStrategy {
        <T> Optional<T> execute(ThrowingSupplier<T> supplier);
    }

    private record ExponentialBackoffRetry(int maxRetries, long initialDelayMs) implements RetryStrategy {

        @Override
            public <T> Optional<T> execute(ThrowingSupplier<T> supplier) {
                for (int retry = 0; retry < maxRetries; retry++) {
                    try {
                        return Optional.of(supplier.get());
                    } catch (Exception e) {
                        if (retry == maxRetries - 1) {
                            log.error("操作失敗，已達到最大重試次數", e);
                            return Optional.empty();
                        }
                        long delayMs = initialDelayMs * (long) Math.pow(2, retry);
                        log.warn("操作失敗，將在{}毫秒後重試. 錯誤: {}", delayMs, e.getMessage());
                        try {
                            TimeUnit.MILLISECONDS.sleep(delayMs);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            return Optional.empty();
                        }
                    }
                }
                return Optional.empty();
            }
        }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}