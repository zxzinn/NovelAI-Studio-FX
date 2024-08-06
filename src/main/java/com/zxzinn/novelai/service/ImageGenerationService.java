package com.zxzinn.novelai.service;

import com.zxzinn.novelai.api.NovelAIAPIClient;
import com.zxzinn.novelai.controller.AbstractGenerationController;
import com.zxzinn.novelai.utils.image.ImageUtils;
import javafx.scene.image.Image;

import java.io.IOException;

public class ImageGenerationService {
    private NovelAIAPIClient apiClient;

    public ImageGenerationService(NovelAIAPIClient apiClient) {
        this.apiClient = apiClient;
    }

    public Image generateImage(AbstractGenerationController controller) throws IOException {
        byte[] responseData = apiClient.generateImage(controller);
        if (responseData == null || responseData.length == 0) {
            return null;
        }

        byte[] imageData;
        try {
            imageData = ImageUtils.extractImageFromZip(responseData);
        } catch (IOException e) {
            // log.info("數據不是 ZIP 格式，假定它是直接的圖像數據");
            imageData = responseData;
        }

        return ImageUtils.byteArrayToImage(imageData);
    }

    public Image generateImg2Img(AbstractGenerationController controller) throws IOException {
        byte[] responseData = apiClient.generateImg2Img(controller);
        if (responseData == null || responseData.length == 0) {
            return null;
        }

        byte[] imageData;
        try {
            imageData = ImageUtils.extractImageFromZip(responseData);
        } catch (IOException e) {
            // log.info("數據不是 ZIP 格式，假定它是直接的圖像數據");
            imageData = responseData;
        }

        return ImageUtils.byteArrayToImage(imageData);
    }
}