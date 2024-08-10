package com.zxzinn.novelai.controller.generation.img2img;

import com.zxzinn.novelai.api.APIClient;
import com.zxzinn.novelai.api.GenerationPayload;
import com.zxzinn.novelai.api.ImageGenerationPayload;
import com.zxzinn.novelai.api.Img2ImgGenerationPayload;
import com.zxzinn.novelai.controller.generation.AbstractGenerationController;
import com.zxzinn.novelai.service.generation.ImageGenerationService;
import com.zxzinn.novelai.utils.common.SettingsManager;
import com.zxzinn.novelai.utils.embed.EmbedProcessor;
import com.zxzinn.novelai.utils.image.ImageUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import lombok.extern.log4j.Log4j2;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Log4j2
public class Img2ImgGeneratorController extends AbstractGenerationController {

    @FXML private Button uploadImageButton;
    @FXML private TextField extraNoiseSeedField;

    private String base64Image;

    public Img2ImgGeneratorController(APIClient apiClient, EmbedProcessor embedProcessor,
                                      SettingsManager settingsManager,
                                      ImageGenerationService imageGenerationService,
                                      ImageUtils imageUtils) {
        super(apiClient, embedProcessor, settingsManager, imageGenerationService, imageUtils);
    }

    @FXML
    @Override
    public void initialize() {
        super.initialize();
        uploadImageButton.setOnAction(event -> handleUploadImage());
        extraNoiseSeedField.setText("0");
    }

    @FXML
    private void handleUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("選擇圖片");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        File selectedFile = fileChooser.showOpenDialog(uploadImageButton.getScene().getWindow());
        if (selectedFile != null) {
            try {
                BufferedImage image = imageUtils.loadImage(selectedFile);
                base64Image = imageUtils.imageToBase64(image);
                log.info("圖片已上傳: {}", selectedFile.getName());
            } catch (IOException e) {
                log.error("上傳圖片時發生錯誤", e);
            }
        }
    }

    @Override
    protected void generateImages() {
        CompletableFuture.runAsync(() -> {
            try {
                if (base64Image == null) {
                    log.error("請先上傳一張圖片");
                    Platform.runLater(() -> {
                        updateButtonState(false);
                        isStopping = false;
                    });
                    return;
                }

                while (isGenerating && !stopRequested && currentGeneratedCount < getMaxCount()) {
                    GenerationPayload payload = createGenerationPayload(
                            positivePromptPreviewArea.getText(),
                            negativePromptPreviewArea.getText()
                    );
                    BufferedImage image = imageGenerationService.generateImage(payload, apiKeyField.getText());

                    if (image != null) {
                        handleGeneratedImage(image);
                    }

                    currentGeneratedCount++;
                    updatePromptPreviewsAsync();
                }
            } catch (IOException e) {
                log.error("生成圖像時發生錯誤：{}", e.getMessage(), e);
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
        Img2ImgGenerationPayload payload = new Img2ImgGenerationPayload();
        payload.setInput(processedPositivePrompt);
        payload.setModel(modelComboBox.getValue());
        payload.setAction("img2img");

        Img2ImgGenerationPayload.Img2ImgGenerationParameters parameters = new Img2ImgGenerationPayload.Img2ImgGenerationParameters();
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
        parameters.setImage(base64Image);
        parameters.setExtra_noise_seed(Long.parseLong(extraNoiseSeedField.getText()));

        payload.setParameters(parameters);
        return payload;
    }
}
