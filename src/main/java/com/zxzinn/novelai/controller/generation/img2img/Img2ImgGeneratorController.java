package com.zxzinn.novelai.controller.generation.img2img;

import com.zxzinn.novelai.api.APIClient;
import com.zxzinn.novelai.api.GenerationPayload;
import com.zxzinn.novelai.api.Img2ImgGenerationPayload;
import com.zxzinn.novelai.controller.generation.AbstractGenerationController;
import com.zxzinn.novelai.service.filemanager.FilePreviewService;
import com.zxzinn.novelai.service.generation.ImageGenerationService;
import com.zxzinn.novelai.utils.common.SettingsManager;
import com.zxzinn.novelai.utils.embed.EmbedProcessor;
import com.zxzinn.novelai.utils.image.ImageUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import lombok.extern.log4j.Log4j2;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Log4j2
public class Img2ImgGeneratorController extends AbstractGenerationController {

    @FXML private Button uploadImageButton;
    @FXML private TextField extraNoiseSeedField;
    @FXML private Slider strengthSlider;
    @FXML private Label strengthLabel;

    private String base64Image;

    public Img2ImgGeneratorController(APIClient apiClient, EmbedProcessor embedProcessor,
                                      SettingsManager settingsManager,
                                      ImageGenerationService imageGenerationService,
                                      ImageUtils imageUtils,
                                      FilePreviewService filePreviewService) {
        super(apiClient, embedProcessor, settingsManager, imageGenerationService, imageUtils, filePreviewService);
    }

    @FXML
    @Override
    public void initialize() {
        super.initialize();
        uploadImageButton.setOnAction(event -> handleUploadImage());
        extraNoiseSeedField.setText("0");
        strengthSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            double roundedValue = Math.round(newValue.doubleValue() * 10.0) / 10.0;
            strengthSlider.setValue(roundedValue);
            strengthLabel.setText(String.format("%.1f", roundedValue));
        });
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
        parameters.setSeed(Long.parseLong(seedField.getText()));
        parameters.setNegative_prompt(processedNegativePrompt);
        parameters.setImage(base64Image);
        parameters.setExtra_noise_seed(Long.parseLong(extraNoiseSeedField.getText()));

        // 新增的參數
        parameters.setStrength(strengthSlider.getValue());
        parameters.setNoise(0);
        parameters.setDynamic_thresholding(false);
        parameters.setControlnet_strength(1.0);
        parameters.setLegacy(false);
        parameters.setAdd_original_image(true);
        parameters.setCfg_rescale(0);
        parameters.setNoise_schedule("native");
        parameters.setLegacy_v3_extend(false);

        payload.setParameters(parameters);
        return payload;
    }

    @Override
    protected void generateImages() {
        if (base64Image == null) {
            log.error("請先上傳一張圖片");
            return;
        }
        super.generateImages();
    }
}