package com.zxzinn.novelai.service.filemanager;

import com.zxzinn.novelai.utils.image.ImageProcessor;
import lombok.extern.log4j.Log4j2;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Log4j2
public class ImageProcessingService {

    public void processImage(File file, String watermarkText, boolean clearMetadata) {
        try {
            BufferedImage image = ImageIO.read(file);
            if (!watermarkText.isEmpty()) {
                ImageProcessor.addWatermark(image, watermarkText);
            }
            if (clearMetadata) {
                ImageProcessor.clearMetadata(image);
            }
            File outputFile = new File(file.getParentFile(), "processed_" + file.getName());
            ImageProcessor.saveImage(image, outputFile);
            log.info("圖像處理完成: {}", outputFile.getName());
        } catch (IOException e) {
            log.error("處理圖像時發生錯誤", e);
            throw new RuntimeException("處理圖像時發生錯誤: " + e.getMessage(), e);
        }
    }
}