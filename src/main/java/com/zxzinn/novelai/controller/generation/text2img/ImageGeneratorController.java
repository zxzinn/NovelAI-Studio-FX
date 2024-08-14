package com.zxzinn.novelai.controller.generation.text2img;

import com.zxzinn.novelai.api.GenerationPayload;
import com.zxzinn.novelai.api.ImageGenerationPayload;
import com.zxzinn.novelai.api.APIClient;
import com.zxzinn.novelai.controller.generation.AbstractGenerationController;
import com.zxzinn.novelai.service.filemanager.FilePreviewService;
import com.zxzinn.novelai.service.generation.ImageGenerationService;
import com.zxzinn.novelai.service.ui.NotificationService;
import com.zxzinn.novelai.utils.common.SettingsManager;
import com.zxzinn.novelai.utils.embed.EmbedProcessor;
import com.zxzinn.novelai.utils.image.ImageUtils;
import javafx.application.Platform;
import javafx.util.Duration;
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
                                    ImageUtils imageUtils,
                                    FilePreviewService filePreviewService) {
        super(apiClient, embedProcessor, settingsManager, imageGenerationService, imageUtils, filePreviewService);
    }

    @Override
    protected void generateImages() {
        CompletableFuture.runAsync(() -> {
            try {
                while (isGenerating && !stopRequested && currentGeneratedCount < getMaxCount()) {
                    if (!promptUpdateLatch.await(5, TimeUnit.SECONDS)) {
                        log.warn("等待提示詞更新超時");
                    }

                    GenerationPayload payload = createGenerationPayload(
                            positivePromptPreviewArea.getText(),
                            negativePromptPreviewArea.getText()
                    );

                    BufferedImage image;
                    for (int retry = 0; true; retry++) {
                        try {
                            image = imageGenerationService.generateImage(payload, apiKeyField.getText());
                            break;
                        } catch (IOException e) {
                            if (retry == MAX_RETRIES - 1) {
                                throw e;
                            }
                            log.warn("生成圖像失敗,將在{}毫秒後重試. 錯誤: {}", RETRY_DELAY, e.getMessage());
                            Thread.sleep(RETRY_DELAY);
                        }
                    }

                    if (image != null) {
                        handleGeneratedImage(image);
                        currentGeneratedCount++;
                        Platform.runLater(() -> NotificationService.showNotification("圖像生成成功！", Duration.seconds(3)));
                    } else {
                        Platform.runLater(() -> NotificationService.showNotification("圖像生成失敗,請稍後重試", Duration.seconds(5)));
                    }

                    promptUpdateLatch = new CountDownLatch(1);
                    updatePromptPreviewsAsync();

                }
            } catch (IOException e) {
                log.error("生成圖像時發生錯誤：{}", e.getMessage(), e);
                Platform.runLater(() -> NotificationService.showNotification("圖像生成過程中發生錯誤", Duration.seconds(5)));
            } catch (InterruptedException e) {
                log.warn("圖像生成過程被中斷");
                Thread.currentThread().interrupt();
            } finally {
                finishGeneration();
            }
        });
    }

    private int getMaxCount() {
        String selectedCount = generateCountComboBox.getValue();
        return "無限".equals(selectedCount) ? Integer.MAX_VALUE : Integer.parseInt(selectedCount);
    }

    @Override
    protected GenerationPayload createGenerationPayload(String processedPositivePrompt, String processedNegativePrompt) {
        ImageGenerationPayload payload = new ImageGenerationPayload();
        payload.setInput(processedPositivePrompt);
        payload.setModel(modelComboBox.getValue());
        payload.setAction("generate");

        GenerationPayload.GenerationParameters parameters = new GenerationPayload.GenerationParameters();
        parameters.setWidth(Integer.parseInt(widthField.getText()));
        parameters.setHeight(Integer.parseInt(heightField.getText()));
        parameters.setScale(Integer.parseInt(ratioField.getText()));
        parameters.setSampler(samplerComboBox.getValue());
        parameters.setSteps(Integer.parseInt(stepsField.getText()));
        parameters.setN_samples(Integer.parseInt(countField.getText()));
        parameters.setUcPreset(false);
        parameters.setQualityToggle(false);
        parameters.setSm(smeaCheckBox.isSelected());
        parameters.setSm_dyn(smeaDynCheckBox.isSelected());
        parameters.setSeed(Long.parseLong(seedField.getText()));
        parameters.setNegative_prompt(processedNegativePrompt);

        payload.setParameters(parameters);
        return payload;
    }
}