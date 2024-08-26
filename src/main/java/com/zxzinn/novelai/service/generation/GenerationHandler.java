package com.zxzinn.novelai.service.generation;

import com.zxzinn.novelai.api.GenerationPayload;
import com.zxzinn.novelai.utils.image.ImageUtils;
import lombok.extern.log4j.Log4j2;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Log4j2
public class GenerationHandler {
    private static final int MAX_RETRIES = 5;
    private static final long RETRY_DELAY = 20000;

    private final ImageGenerationService imageGenerationService;

    public GenerationHandler(ImageGenerationService imageGenerationService) {
        this.imageGenerationService = imageGenerationService;
    }

    public Optional<BufferedImage> generateImageWithRetry(GenerationPayload payload, String apiKey) {
        for (int retry = 0; retry < MAX_RETRIES; retry++) {
            try {
                return Optional.of(imageGenerationService.generateImage(payload, apiKey));
            } catch (IOException e) {
                if (retry == MAX_RETRIES - 1) {
                    log.error("生成圖像失敗，已達到最大重試次數", e);
                    return Optional.empty();
                }
                log.warn("生成圖像失敗,將在{}毫秒後重試. 錯誤: {}", RETRY_DELAY, e.getMessage());
                try {
                    Thread.sleep(RETRY_DELAY);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    public Optional<File> saveImageToFile(BufferedImage image, String outputDir, String timeStamp, int currentGeneratedCount) {
        try {
            Path outputPath = Paths.get(outputDir);
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
            }

            String fileName = String.format("generated_image_%s_%d.png", timeStamp.replace(":", "-"), currentGeneratedCount);
            File outputFile = outputPath.resolve(fileName).toFile();
            ImageUtils.saveImage(image, outputFile);
            return Optional.of(outputFile);
        } catch (IOException e) {
            log.error("保存圖像時發生錯誤：{}", e.getMessage(), e);
            return Optional.empty();
        }
    }
}
