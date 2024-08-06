package com.zxzinn.novelai.service;

import com.zxzinn.novelai.api.APIClient;
import com.zxzinn.novelai.api.ImageGenerationPayload;
import com.zxzinn.novelai.api.Img2ImgGenerationPayload;
import com.zxzinn.novelai.api.NovelAIAPIClient;
import com.zxzinn.novelai.controller.AbstractGenerationController;
import com.zxzinn.novelai.utils.image.ImageUtils;
import javafx.scene.image.Image;

import java.io.IOException;

public class ImageGenerationService {
    private final APIClient apiClient;
    private final ImageUtils imageUtils;

    public ImageGenerationService(APIClient apiClient, ImageUtils imageUtils) {
        this.apiClient = apiClient;
        this.imageUtils = imageUtils;
    }

    public Image generateImage(ImageGenerationPayload payload, String apiKey) throws IOException {
        byte[] responseData = apiClient.generateImage(payload, apiKey);
        return processResponseData(responseData);
    }

    public Image generateImg2Img(Img2ImgGenerationPayload payload, String apiKey) throws IOException {
        byte[] responseData = apiClient.generateImg2Img(payload, apiKey);
        return processResponseData(responseData);
    }

    private Image processResponseData(byte[] responseData) throws IOException {
        if (responseData == null || responseData.length == 0) {
            return null;
        }

        byte[] imageData;
        try {
            imageData = imageUtils.extractImageFromZip(responseData);
        } catch (IOException e) {
            imageData = responseData;
        }

        return imageUtils.byteArrayToImage(imageData);
    }
}