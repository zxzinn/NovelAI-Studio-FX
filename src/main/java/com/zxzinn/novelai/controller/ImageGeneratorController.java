package com.zxzinn.novelai.controller;

import com.zxzinn.novelai.Application;
import com.zxzinn.novelai.api.APIClient;
import com.zxzinn.novelai.api.ImageGenerationPayload;
import com.zxzinn.novelai.service.ImageGenerationService;
import com.zxzinn.novelai.utils.SettingsManager;
import com.zxzinn.novelai.utils.embed.EmbedProcessor;
import com.zxzinn.novelai.utils.image.ImageUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Window;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@Log4j2
public class ImageGeneratorController extends AbstractGenerationController {

    @FXML public Button generateButton;
    @FXML public ScrollPane mainScrollPane;
    @FXML public VBox historyImagesContainer;

    private int currentGeneratedCount = 0;
    @Setter
    private Window mainWindow;
    private final SettingsManager settingsManager;
    private final ImageGenerationService imageGenerationService;
    private final List<Image> generatedImages;
    private final ImageUtils imageUtils;

    public ImageGeneratorController(APIClient apiClient, EmbedProcessor embedProcessor,
                                    SettingsManager settingsManager,
                                    ImageGenerationService imageGenerationService,
                                    ImageUtils imageUtils) {
        super(apiClient, embedProcessor);
        this.settingsManager = settingsManager;
        this.imageGenerationService = imageGenerationService;
        this.imageUtils = imageUtils;
        this.generatedImages = new ArrayList<>();
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
            double deltaY = event.getDeltaY();
            double zoomFactor = 1.1;
            double direction = deltaY > 0 ? 1 : -1;
            double scale = Math.pow(zoomFactor, direction);

            mainImageView.setScaleX(mainImageView.getScaleX() * scale);
            mainImageView.setScaleY(mainImageView.getScaleY() * scale);

            event.consume();
        });
    }

    public void setMainWindow(Window window) {
        this.mainWindow = window;
        if (mainWindow != null) {
            mainWindow.setOnCloseRequest(e -> settingsManager.shutdown());
        }
    }

    @FXML
    @Override
    protected void handleGenerate() {
        generateButton.setDisable(true);
        currentGeneratedCount = 0;
        String positivePrompt = positivePromptArea.getText();
        String negativePrompt = negativePromptArea.getText();
        generateImages(positivePrompt, negativePrompt);
    }

    private void generateImages(String positivePrompt, String negativePrompt) {
        new Thread(() -> {
            try {
                String processedPositivePrompt = embedProcessor.processPrompt(positivePrompt);
                String processedNegativePrompt = embedProcessor.processPrompt(negativePrompt);

                updatePromptPreviewsAsync(processedPositivePrompt, processedNegativePrompt);

                ImageGenerationPayload payload = createImageGenerationPayload(processedPositivePrompt, processedNegativePrompt);
                Image image = imageGenerationService.generateImage(payload, apiKeyField.getText());

                if (image != null) {
                    handleGeneratedImage(image);
                }

                currentGeneratedCount++;
                String selectedCount = generateCountComboBox.getValue();
                int maxCount = "無限".equals(selectedCount) ? Integer.MAX_VALUE : Integer.parseInt(selectedCount);
                if (currentGeneratedCount < maxCount) {
                    generateImages(positivePromptArea.getText(), negativePromptArea.getText());
                }
            } catch (IOException e) {
                log.error("生成圖像時發生錯誤：" + e.getMessage(), e);
            } finally {
                Platform.runLater(() -> generateButton.setDisable(false));
            }
        }).start();
    }

    private void updatePromptPreviewsAsync(String processedPositivePrompt, String processedNegativePrompt) {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            positivePromptPreviewArea.setText(processedPositivePrompt);
            negativePromptPreviewArea.setText(processedNegativePrompt);
            latch.countDown();
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            log.error("等待預覽區域更新時發生中斷：" + e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
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

    private void handleGeneratedImage(Image image) {
        generatedImages.add(image);

        Platform.runLater(() -> {
            // 獲取當前時間
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String timeStamp = now.format(formatter);

            // 創建一個新的 Canvas 來繪製圖片和文字
            Canvas canvas = new Canvas(image.getWidth(), image.getHeight());
            GraphicsContext gc = canvas.getGraphicsContext2D();

            // 繪製原始圖片
            gc.drawImage(image, 0, 0);

            // 設置文字樣式
            gc.setFill(Color.WHITE);
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(1);
            gc.setFont(new Font("Arial", 20));

            // 繪製時間戳和批次號
            String text = timeStamp + " _" + (currentGeneratedCount);
            gc.strokeText(text, 10, 30);
            gc.fillText(text, 10, 30);

            // 將 Canvas 轉換為 Image
            Image watermarkedImage = canvas.snapshot(null, null);

            // 更新 UI
            mainImageView.setImage(watermarkedImage);
            mainImageView.setPreserveRatio(true);
            mainImageView.setSmooth(true);
            mainImageView.fitWidthProperty().bind(((AnchorPane) mainImageView.getParent()).widthProperty());
            mainImageView.fitHeightProperty().bind(((AnchorPane) mainImageView.getParent()).heightProperty());

            addImageToHistory(watermarkedImage);

            try {
                // 修改文件名以包含時間戳和批次號
                String fileName = "generated_image_" + timeStamp.replace(":", "-") + "_" + (currentGeneratedCount + 1) + ".png";
                imageUtils.saveImage(watermarkedImage, fileName);
            } catch (IOException e) {
                log.error("保存圖像時發生錯誤：" + e.getMessage(), e);
            }
        });
    }

    private void addImageToHistory(Image image) {
        double aspectRatio = image.getWidth() / image.getHeight();
        ImageView historyImageView = new ImageView(image);
        historyImageView.setPreserveRatio(true);
        historyImageView.setSmooth(true);
        historyImageView.setFitWidth(150);
        historyImageView.setFitHeight(150 / aspectRatio);

        historyImageView.setOnMouseClicked(event -> mainImageView.setImage(image));

        historyImagesContainer.getChildren().add(historyImageView);
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