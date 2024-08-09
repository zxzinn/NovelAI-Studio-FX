package com.zxzinn.novelai.service.generation;

import com.zxzinn.novelai.api.APIClient;
import com.zxzinn.novelai.api.GenerationPayload;
import com.zxzinn.novelai.api.ImageGenerationPayload;
import com.zxzinn.novelai.utils.image.ImageUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ImageGenerationService {
    private final APIClient apiClient;
    private final ImageUtils imageUtils;

    public ImageGenerationService(APIClient apiClient, ImageUtils imageUtils) {
        this.apiClient = apiClient;
        this.imageUtils = imageUtils;
    }

    public BufferedImage generateImage(GenerationPayload payload, String apiKey) throws IOException {
        byte[] responseData = apiClient.generateImage(payload, apiKey);
        return processResponseData(responseData);
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