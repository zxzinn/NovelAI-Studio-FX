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
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Log4j2
public class Img2ImgGeneratorController extends AbstractGenerationController {
    @FXML public VBox historyImagesContainer;
    @FXML public ImageView mainImageView;
    @FXML public Button generateButton;
    @FXML public TextField extraNoiseSeedField;
    @FXML public Button uploadImageButton;
    public ScrollPane mainScrollPane;

    private String base64Image;
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
    public void initialize() {
        super.initialize();
        uploadImageButton.setOnAction(event -> handleUploadImage());
    }

    @FXML
    @Override
    protected void handleGenerate() {
        generateButton.setDisable(true);
        CompletableFuture.runAsync(() -> {
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
        });
    }

    private Img2ImgGenerationPayload createImg2ImgGenerationPayload() {
        Img2ImgGenerationPayload payload = new Img2ImgGenerationPayload();
        // 設置payload的各個屬性
        payload.setInput(positivePromptArea.getText());
        payload.setModel(modelComboBox.getValue());
        payload.setAction("img2img");

        Img2ImgGenerationPayload.Img2ImgGenerationParameters parameters = new Img2ImgGenerationPayload.Img2ImgGenerationParameters();
        parameters.setWidth(Integer.parseInt(widthField.getText()));
        parameters.setHeight(Integer.parseInt(heightField.getText()));
        parameters.setScale(Integer.parseInt(ratioField.getText()));
        parameters.setSampler(samplerComboBox.getValue());
        parameters.setSteps(Integer.parseInt(stepsField.getText()));
        parameters.setN_samples(Integer.parseInt(countField.getText()));
        parameters.setSeed(Long.parseLong(seedField.getText()));
        parameters.setExtra_noise_seed(Long.parseLong(extraNoiseSeedField.getText()));
        parameters.setNegative_prompt(negativePromptArea.getText());
        parameters.setImage(base64Image);

        payload.setParameters(parameters);
        return payload;
    }

    private void handleGeneratedImage(Image image) {
        Platform.runLater(() -> {
            mainImageView.setImage(image);
            addImageToHistory(image);
            try {
                ImageUtils.saveImage(image, "generated_img2img.png");
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
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("選擇圖片");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("圖片文件", "*.png", "*.jpg", "*.jpeg")
        );
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            try {
                base64Image = imageUtils.fileToBase64(selectedFile);
                Image image = new Image(selectedFile.toURI().toString());
                mainImageView.setImage(image);
            } catch (IOException e) {
                log.error("上傳圖片時發生錯誤：" + e.getMessage(), e);
            }
        }
    }
}
