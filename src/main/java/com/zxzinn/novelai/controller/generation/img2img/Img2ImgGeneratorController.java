package com.zxzinn.novelai.controller.generation.img2img;

import com.zxzinn.novelai.api.APIClient;
import com.zxzinn.novelai.api.ImageGenerationPayload;
import com.zxzinn.novelai.controller.generation.AbstractGenerationController;
import com.zxzinn.novelai.service.generation.ImageGenerationService;
import com.zxzinn.novelai.utils.common.SettingsManager;
import com.zxzinn.novelai.utils.embed.EmbedProcessor;
import com.zxzinn.novelai.utils.image.ImageUtils;
import javafx.application.Platform;
import lombok.extern.log4j.Log4j2;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Log4j2
public class Img2ImgGeneratorController extends AbstractGenerationController {

    public Img2ImgGeneratorController(APIClient apiClient, EmbedProcessor embedProcessor,
                                      SettingsManager settingsManager,
                                      ImageGenerationService imageGenerationService,
                                      ImageUtils imageUtils) {
        super(apiClient, embedProcessor, settingsManager, imageGenerationService, imageUtils);
    }

    @Override
    protected void generateImages() {
        CompletableFuture.runAsync(() -> {
            try {
                ImageGenerationPayload payload = createImageGenerationPayload(
                        positivePromptPreviewArea.getText(),
                        negativePromptPreviewArea.getText()
                );
                BufferedImage image = imageGenerationService.generateImage(payload, apiKeyField.getText());

                if (image != null) {
                    handleGeneratedImage(image);
                }

                currentGeneratedCount++;
                updatePromptPreviewsAsync();
                String selectedCount = generateCountComboBox.getValue();
                int maxCount = "無限".equals(selectedCount) ? Integer.MAX_VALUE : Integer.parseInt(selectedCount);
                if (currentGeneratedCount < maxCount) {
                    generateImages();
                }
            } catch (IOException e) {
                log.error("生成圖像時發生錯誤：{}", e.getMessage(), e);
            } finally {
                Platform.runLater(() -> generateButton.setDisable(false));
            }
        });
    }

    // 如果 Img2Img 需要額外的方法或覆寫，可以在這裡添加
}