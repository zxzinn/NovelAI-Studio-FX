package com.zxzinn.novelai.service.generation;

import com.zxzinn.novelai.api.GenerationPayload;
import com.zxzinn.novelai.api.APIClient;
import com.zxzinn.novelai.utils.image.ImageUtils;
import lombok.extern.log4j.Log4j2;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@Log4j2
public class ImageGenerationService {
    private final APIClient apiClient;
    private final ImageUtils imageUtils;

    public ImageGenerationService(APIClient apiClient, ImageUtils imageUtils) {
        this.apiClient = apiClient;
        this.imageUtils = imageUtils;
    }

    public BufferedImage generateImage(GenerationPayload payload, String apiKey) throws IOException {
        int maxRetries = 3;
        int retryDelay = 5000;

        for (int i = 0; i < maxRetries; i++) {
            try {
                byte[] responseData = apiClient.generateImage(payload, apiKey);
                return processResponseData(responseData);
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

    private BufferedImage processResponseData(byte[] responseData) throws IOException {
        if (responseData == null || responseData.length == 0) {
            return null;
        }

        BufferedImage image;
        try {
            image = imageUtils.extractImageFromZip(responseData);
        } catch (IOException e) {
            image = ImageIO.read(new ByteArrayInputStream(responseData));
        }

        return image;
    }
}