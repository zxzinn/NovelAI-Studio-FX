package com.zxzinn.novelai.service.filemanager;

import com.zxzinn.novelai.utils.image.ImageProcessor;
import javafx.application.Platform;

import lombok.extern.log4j.Log4j2;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


@Log4j2
public class ImageProcessingService {

    private final ExecutorService executorService;

    public ImageProcessingService() {
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    public void clearMetadataForFiles(List<File> files, Consumer<Double> progressCallback,
                                      Runnable onComplete, Consumer<String> onError) {
        AtomicInteger processedCount = new AtomicInteger(0);
        int totalFiles = files.size();

        for (File file : files) {
            executorService.submit(() -> {
                try {
                    clearMetadata(file);
                    int completed = processedCount.incrementAndGet();
                    Platform.runLater(() -> progressCallback.accept((double) completed / totalFiles));
                    if (completed == totalFiles) {
                        Platform.runLater(onComplete);
                    }
                } catch (Exception e) {
                    log.error("清除元數據時發生錯誤: {}", file.getName(), e);
                    Platform.runLater(() -> onError.accept(e.getMessage()));
                }
            });
        }
    }

    private void clearMetadata(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            throw new IOException("無法讀取圖像文件: " + file.getName());

        }

        ImageProcessor.clearMetadata(image);

        File cleanedDir = new File(file.getParentFile(), "cleaned");
        if (!cleanedDir.exists() && !cleanedDir.mkdir()) {
            throw new IOException("無法創建 cleaned 目錄");
        }

        File outputFile = new File(cleanedDir, file.getName());
        ImageProcessor.saveImage(image, outputFile);
        log.info("已清除文件的元數據: {}", outputFile.getName());
    }

    public void shutdown() {
        executorService.shutdown();
    }
}