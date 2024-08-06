package com.zxzinn.novelai.controller;

import com.zxzinn.novelai.api.APIClient;
import com.zxzinn.novelai.api.Img2ImgGenerationPayload;
import com.zxzinn.novelai.service.ImageGenerationService;
import com.zxzinn.novelai.utils.SettingsManager;
import com.zxzinn.novelai.utils.embed.EmbedProcessor;
import com.zxzinn.novelai.utils.image.ImageUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;

@Log4j2
public class Img2ImgGeneratorController extends AbstractGenerationController {
    public VBox historyImagesContainer;
    public ImageView mainImageView;
    public Button generateButton;
    @FXML public TextField extraNoiseSeedField;
    @FXML public Button uploadImageButton;

    public String base64Image;
    private final ImageGenerationService imageGenerationService;
    private final ImageUtils imageUtils;
    private final SettingsManager settingsManager;

    public Img2ImgGeneratorController(APIClient apiClient, EmbedProcessor embedProcessor,
                                      ImageGenerationService imageGenerationService,
                                      ImageUtils imageUtils, SettingsManager settingsManager) {
        super(apiClient, embedProcessor);
        this.imageGenerationService = imageGenerationService;
        this.imageUtils = imageUtils;
        this.settingsManager = settingsManager;
    }

    @FXML
    @Override
    protected void handleGenerate() {
        generateButton.setDisable(true);
        new Thread(() -> {
            try {
                Img2ImgGenerationPayload payload = createImg2ImgGenerationPayload();
                Image image = imageGenerationService.generateImg2Img(payload, apiKeyField.getText());
                if (image != null) {
                    handleGeneratedImage(image);
                }
            } catch (IOException e) {
                log.error("生成圖像時發生錯誤：" + e.getMessage(), e);
            } finally {
                Platform.runLater(() -> generateButton.setDisable(false));
            }
        }).start();
    }

    private Img2ImgGenerationPayload createImg2ImgGenerationPayload() {
        Img2ImgGenerationPayload payload = new Img2ImgGenerationPayload();
        // 設置payload的各個屬性
        // ...
        return payload;
    }

    private void handleGeneratedImage(Image image) {
        Platform.runLater(() -> {
            mainImageView.setImage(image);
            addImageToHistory(image);
            try {
                imageUtils.saveImage(image, "generated_img2img.png");
            } catch (IOException e) {
                log.error("保存圖像時發生錯誤：" + e.getMessage(), e);
            }
        });
    }

    private void addImageToHistory(Image image) {
        ImageView historyImageView = new ImageView(image);
        historyImageView.setFitWidth(150);
        historyImageView.setFitHeight(150);
        historyImageView.setOnMouseClicked(event -> mainImageView.setImage(image));
        historyImagesContainer.getChildren().add(historyImageView);
    }

    @FXML
    private void handleUploadImage() {
        // 實現圖片上傳邏輯
    }
}
