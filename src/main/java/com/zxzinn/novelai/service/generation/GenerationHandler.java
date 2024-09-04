package com.zxzinn.novelai.service.generation;

import com.google.inject.Inject;
import com.zxzinn.novelai.api.GenerationPayload;
import com.zxzinn.novelai.utils.strategy.ExponentialBackoffRetry;
import com.zxzinn.novelai.utils.strategy.RetryStrategy;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Log4j2
public class GenerationHandler {
    private static final int MAX_RETRIES = 5;
    private static final long INITIAL_RETRY_DELAY_MS = 20000;
    private static final String FILE_NAME_FORMAT = "NovelAI_FX_generated_%s.png";
    private static final DateTimeFormatter FILE_NAME_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final ImageGenerationService imageGenerationService;
    private final RetryStrategy retryStrategy;

    @Inject
    public GenerationHandler(ImageGenerationService imageGenerationService) {
        this.imageGenerationService = imageGenerationService;
        this.retryStrategy = new ExponentialBackoffRetry(MAX_RETRIES, INITIAL_RETRY_DELAY_MS);
    }

    public Optional<byte[]> generateImageWithRetry(GenerationPayload payload, String apiKey) {
        return retryStrategy.execute(() -> {
            Optional<byte[]> result = imageGenerationService.generateImage(payload, apiKey);
            return result.orElseThrow(() -> new RuntimeException("Image generation failed"));
        });
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
        Files.createDirectories(outputPath);
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
}