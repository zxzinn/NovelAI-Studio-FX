package com.zxzinn.novelai.service.filemanager;

import com.zxzinn.novelai.utils.image.ImageProcessor;
import javafx.concurrent.Task;
import lombok.extern.log4j.Log4j2;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
public class ImageProcessingService {

    private final ExecutorService executorService;

    public ImageProcessingService() {
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    public Task<Void> processImages(List<File> files, String watermarkText, boolean clearMetadata) {
        return new Task<>() {
            @Override
            protected Void call() {
                AtomicInteger processedCount = new AtomicInteger(0);
                files.forEach(file -> executorService.submit(() -> {
                    processImage(file, watermarkText, clearMetadata);
                    updateProgress(processedCount.incrementAndGet(), files.size());
                }));
                return null;
            }
        };
    }

    private void processImage(File file, String watermarkText, boolean clearMetadata) {
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                log.error("無法讀取圖像文件: {}", file.getName());
                return;
            }

            if (!watermarkText.isEmpty()) {
                ImageProcessor.addWatermark(image, watermarkText);
            }
            if (clearMetadata) {
                ImageProcessor.clearMetadata(image);
            }

            File cleanedDir = new File(file.getParentFile(), "cleaned");
            if (!cleanedDir.exists() && !cleanedDir.mkdir()) {
                log.error("無法創建 cleaned 目錄");
                return;
            }

            File outputFile = new File(cleanedDir, file.getName());
            ImageProcessor.saveImage(image, outputFile);
            log.info("圖像處理完成: {}", outputFile.getName());
        } catch (IOException e) {
            log.error("處理圖像時發生錯誤: {}", file.getName(), e);
        }
    }

    public Task<Void> clearMetadataForFiles(List<File> files) {
        return new Task<>() {
            @Override
            protected Void call() {
                AtomicInteger processedCount = new AtomicInteger(0);
                files.forEach(file -> executorService.submit(() -> {
                    clearMetadata(file);
                    updateProgress(processedCount.incrementAndGet(), files.size());
                }));
                return null;
            }
        };
    }

    private void clearMetadata(File file) {
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                log.error("無法讀取圖像文件: {}", file.getName());
                return;
            }

            ImageProcessor.clearMetadata(image);

            File cleanedDir = new File(file.getParentFile(), "cleaned");
            if (!cleanedDir.exists() && !cleanedDir.mkdir()) {
                log.error("無法創建 cleaned 目錄");
                return;
            }

            File outputFile = new File(cleanedDir, file.getName());
            ImageProcessor.saveImage(image, outputFile);
            log.info("已清除文件的元數據: {}", outputFile.getName());
        } catch (IOException e) {
            log.error("清除元數據時發生錯誤: {}", file.getName(), e);
        }
    }

    public void shutdown() {
        executorService.shutdown();
    }
}