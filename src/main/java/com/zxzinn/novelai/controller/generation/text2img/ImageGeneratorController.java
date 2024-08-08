package com.zxzinn.novelai.controller.generation.text2img;

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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Log4j2
public class ImageGeneratorController extends AbstractGenerationController {

    public ImageGeneratorController(APIClient apiClient, EmbedProcessor embedProcessor,
                                    SettingsManager settingsManager,
                                    ImageGenerationService imageGenerationService,
                                    ImageUtils imageUtils) {
        super(apiClient, embedProcessor, settingsManager, imageGenerationService, imageUtils);
    }

    @Override
    protected void generateImages() {
        CompletableFuture.runAsync(() -> {
            try {
                // 等待提示詞更新完成，最多等待5秒
                if (!promptUpdateLatch.await(5, TimeUnit.SECONDS)) {
                    log.warn("等待提示詞更新超時");
                }

                ImageGenerationPayload payload = createImageGenerationPayload(
                        positivePromptPreviewArea.getText(),
                        negativePromptPreviewArea.getText()
                );
                BufferedImage image = imageGenerationService.generateImage(payload, apiKeyField.getText());

                if (image != null) {
                    handleGeneratedImage(image);
                }

                currentGeneratedCount++;
                String selectedCount = generateCountComboBox.getValue();
                int maxCount = "無限".equals(selectedCount) ? Integer.MAX_VALUE : Integer.parseInt(selectedCount);
                if (currentGeneratedCount < maxCount) {
                    promptUpdateLatch = new CountDownLatch(1);
                    updatePromptPreviewsAsync();
                    generateImages();
                }
            } catch (IOException | InterruptedException e) {
                log.error("生成圖像時發生錯誤：{}", e.getMessage(), e);
            } finally {
                Platform.runLater(() -> generateButton.setDisable(false));
            }
        });
    }
}