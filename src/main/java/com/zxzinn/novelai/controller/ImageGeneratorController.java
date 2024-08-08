package com.zxzinn.novelai.controller;

import com.zxzinn.novelai.api.APIClient;
import com.zxzinn.novelai.api.ImageGenerationPayload;
import com.zxzinn.novelai.service.ImageGenerationService;
import com.zxzinn.novelai.utils.SettingsManager;
import com.zxzinn.novelai.utils.embed.EmbedProcessor;
import com.zxzinn.novelai.utils.image.ImageUtils;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import lombok.extern.log4j.Log4j2;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Log4j2
public class ImageGeneratorController extends AbstractGenerationController {

    @FXML private Button generateButton;
    @FXML private ScrollPane mainScrollPane;
    @FXML private VBox historyImagesContainer;
    @FXML private ImageView mainImageView;

    private int currentGeneratedCount = 0;
    private final SettingsManager settingsManager;
    private final ImageGenerationService imageGenerationService;
    private final ImageUtils imageUtils;
    private CountDownLatch promptUpdateLatch;

    public ImageGeneratorController(APIClient apiClient, EmbedProcessor embedProcessor,
                                    SettingsManager settingsManager,
                                    ImageGenerationService imageGenerationService,
                                    ImageUtils imageUtils) {
        super(apiClient, embedProcessor);
        this.settingsManager = settingsManager;
        this.imageGenerationService = imageGenerationService;
        this.imageUtils = imageUtils;
    }

    @FXML
    public void initialize() {
        super.initialize();
        loadSettings();
        setupListeners();
        setupZoomHandler();
        updatePromptPreviews();
    }

    private void setupZoomHandler() {
        mainScrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.isControlDown()) {
                double deltaY = event.getDeltaY();
                double zoomFactor = 1.1;
                double direction = deltaY > 0 ? 1 : -1;
                double scale = Math.pow(zoomFactor, direction);

                mainImageView.setScaleX(mainImageView.getScaleX() * scale);
                mainImageView.setScaleY(mainImageView.getScaleY() * scale);

                event.consume();
            }
        });
    }

    @FXML
    @Override
    protected void handleGenerate() {
        generateButton.setDisable(true);
        currentGeneratedCount = 0;
        promptUpdateLatch = new CountDownLatch(1);
        generateImages();
    }

    private void generateImages() {
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

    private void handleGeneratedImage(BufferedImage image) {
        Platform.runLater(() -> {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String timeStamp = now.format(formatter);

            BufferedImage watermarkedImage = addWatermark(image, timeStamp);

            Image fxImage = SwingFXUtils.toFXImage(watermarkedImage, null);
            mainImageView.setImage(fxImage);
            mainImageView.setPreserveRatio(true);
            mainImageView.setSmooth(true);
            mainImageView.fitWidthProperty().bind(((AnchorPane) mainImageView.getParent()).widthProperty());
            mainImageView.fitHeightProperty().bind(((AnchorPane) mainImageView.getParent()).heightProperty());

            addImageToHistory(fxImage);

            try {
                String fileName = "generated_image_" + timeStamp.replace(":", "-") + "_" + (currentGeneratedCount) + ".png";
                ImageUtils.saveImage(watermarkedImage, fileName);
            } catch (IOException e) {
                log.error("保存圖像時發生錯誤：" + e.getMessage(), e);
            }
        });
    }

    private void updatePromptPreviewsAsync() {
        Platform.runLater(() -> {
            positivePromptPreviewArea.setText(embedProcessor.processPrompt(positivePromptArea.getText()));
            negativePromptPreviewArea.setText(embedProcessor.processPrompt(negativePromptArea.getText()));
            promptUpdateLatch.countDown();
        });
    }

    private ImageGenerationPayload createImageGenerationPayload(String processedPositivePrompt, String processedNegativePrompt) {
        ImageGenerationPayload payload = new ImageGenerationPayload();
        payload.setInput(processedPositivePrompt);
        payload.setModel(modelComboBox.getValue());
        payload.setAction("generate");

        ImageGenerationPayload.ImageGenerationParameters parameters = new ImageGenerationPayload.ImageGenerationParameters();
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

    private BufferedImage addWatermark(BufferedImage image, String timeStamp) {
        Graphics2D g2d = image.createGraphics();

        g2d.setColor(java.awt.Color.WHITE);
        g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 20));

        String text = timeStamp + " _" + (currentGeneratedCount);
        g2d.drawString(text, 10, 30);

        g2d.dispose();

        return image;
    }

    private void addImageToHistory(Image image) {
        double aspectRatio = image.getWidth() / image.getHeight();
        ImageView historyImageView = new ImageView(image);
        historyImageView.setPreserveRatio(true);
        historyImageView.setSmooth(true);
        historyImageView.setFitWidth(150);
        historyImageView.setFitHeight(150 / aspectRatio);

        historyImageView.setOnMouseClicked(event -> mainImageView.setImage(image));

        historyImagesContainer.getChildren().addFirst(historyImageView);
    }

    private void loadSettings() {
        apiKeyField.setText(settingsManager.getString("apiKey", ""));
        modelComboBox.setValue(settingsManager.getString("model", "nai-diffusion-3"));
        widthField.setText(String.valueOf(settingsManager.getInt("width", 832)));
        heightField.setText(String.valueOf(settingsManager.getInt("height", 1216)));
        samplerComboBox.setValue(settingsManager.getString("sampler", "k_euler"));
        smeaCheckBox.setSelected(settingsManager.getBoolean("smea", true));
        smeaDynCheckBox.setSelected(settingsManager.getBoolean("smeaDyn", false));
        stepsField.setText(String.valueOf(settingsManager.getInt("steps", 28)));
        seedField.setText(String.valueOf(settingsManager.getInt("seed", 0)));
        generateCountComboBox.setValue(settingsManager.getString("generateCount", "1"));
        positivePromptArea.setText(settingsManager.getString("positivePrompt", ""));
        negativePromptArea.setText(settingsManager.getString("negativePrompt", ""));
    }

    private void setupListeners() {
        apiKeyField.textProperty().addListener((obs, oldVal, newVal) ->
                settingsManager.setString("apiKey", newVal));
        modelComboBox.valueProperty().addListener((obs, oldVal, newVal) ->
                settingsManager.setString("model", newVal));
        widthField.textProperty().addListener((obs, oldVal, newVal) ->
                settingsManager.setInt("width", Integer.parseInt(newVal)));
        heightField.textProperty().addListener((obs, oldVal, newVal) ->
                settingsManager.setInt("height", Integer.parseInt(newVal)));
        samplerComboBox.valueProperty().addListener((obs, oldVal, newVal) ->
                settingsManager.setString("sampler", newVal));
        smeaCheckBox.selectedProperty().addListener((obs, oldVal, newVal) ->
                settingsManager.setBoolean("smea", newVal));
        smeaDynCheckBox.selectedProperty().addListener((obs, oldVal, newVal) ->
                settingsManager.setBoolean("smeaDyn", newVal));
        stepsField.textProperty().addListener((obs, oldVal, newVal) ->
                settingsManager.setInt("steps", Integer.parseInt(newVal)));
        seedField.textProperty().addListener((obs, oldVal, newVal) ->
                settingsManager.setInt("seed", Integer.parseInt(newVal)));
        generateCountComboBox.valueProperty().addListener((obs, oldVal, newVal) ->
                settingsManager.setString("generateCount", newVal));
        positivePromptArea.textProperty().addListener((obs, oldVal, newVal) ->
                settingsManager.setString("positivePrompt", newVal));
        negativePromptArea.textProperty().addListener((obs, oldVal, newVal) ->
                settingsManager.setString("negativePrompt", newVal));

        positivePromptArea.textProperty().addListener((observable, oldValue, newValue) ->
                updatePromptPreview(newValue, positivePromptPreviewArea));

        negativePromptArea.textProperty().addListener((observable, oldValue, newValue) ->
                updatePromptPreview(newValue, negativePromptPreviewArea));
    }

    private void updatePromptPreview(String newValue, TextArea previewArea) {
        String processedPrompt = embedProcessor.processPrompt(newValue);
        previewArea.setText(processedPrompt);
    }

    private void updatePromptPreviews() {
        updatePromptPreview(positivePromptArea.getText(), positivePromptPreviewArea);
        updatePromptPreview(negativePromptArea.getText(), negativePromptPreviewArea);
    }
}