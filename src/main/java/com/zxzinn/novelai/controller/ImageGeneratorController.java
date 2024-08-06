package com.zxzinn.novelai.controller;

import com.zxzinn.novelai.Application;
import com.zxzinn.novelai.service.ImageGenerationService;
import com.zxzinn.novelai.utils.SettingsManager;
import com.zxzinn.novelai.utils.embed.EmbedProcessor;
import com.zxzinn.novelai.utils.image.ImageUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

@Log4j2
public class ImageGeneratorController extends AbstractGenerationController {

    @FXML public Button generateButton;
    @FXML public ScrollPane mainScrollPane;

    private EmbedProcessor embedProcessor;
    private int currentGeneratedCount = 0;
    @Setter
    private Window mainWindow;
    private SettingsManager settingsManager;
    private ImageGenerationService imageGenerationService;

    @FXML
    public void initialize() {
        super.initialize();
        embedProcessor = new EmbedProcessor();
        settingsManager = Application.getSettingsManager();
        imageGenerationService = new ImageGenerationService(apiClient);
        loadSettings();
        setupListeners();
        setupZoomHandler();
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
            mainWindow.setOnCloseRequest(e -> {
                settingsManager.shutdown();
            });
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
                    return;
                }

                Image image = imageGenerationService.generateImage(this);
                if (image == null) {
                    return;
                }

                Platform.runLater(() -> {
                    mainImageView.setImage(image);
                    mainImageView.setPreserveRatio(true);
                    mainImageView.setSmooth(true);
                    mainImageView.fitWidthProperty().bind(((AnchorPane) mainImageView.getParent()).widthProperty());
                    mainImageView.fitHeightProperty().bind(((AnchorPane) mainImageView.getParent()).heightProperty());

                    double aspectRatio = image.getWidth() / image.getHeight();
                    ImageView historyImageView = new ImageView(image);
                    historyImageView.setPreserveRatio(true);
                    historyImageView.setSmooth(true);
                    historyImageView.setFitWidth(150);
                    historyImageView.setFitHeight(150 / aspectRatio);
                    historyImagesContainer.getChildren().add(historyImageView);

                    try {
                        ImageUtils.saveImage(image, "generated_image_" + currentGeneratedCount + ".png");
                    } catch (IOException e) {
                        log.error("保存圖像時發生錯誤：" + e.getMessage(), e);
                    }
                });

                currentGeneratedCount++;
                String selectedCount = generateCountComboBox.getValue().toString();
                int maxCount = selectedCount.equals("無限") ? Integer.MAX_VALUE : Integer.parseInt(selectedCount);
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

        positivePromptArea.textProperty().addListener((observable, oldValue, newValue) -> {
            String processedPrompt = embedProcessor.processPrompt(newValue);
            positivePromptPreviewArea.setText(processedPrompt);
        });

        negativePromptArea.textProperty().addListener((observable, oldValue, newValue) -> {
            String processedPrompt = embedProcessor.processPrompt(newValue);
            negativePromptPreviewArea.setText(processedPrompt);
        });
    }
}