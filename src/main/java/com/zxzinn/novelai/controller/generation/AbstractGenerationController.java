package com.zxzinn.novelai.controller.generation;

import com.zxzinn.novelai.api.APIClient;
import com.zxzinn.novelai.api.GenerationPayload;
import com.zxzinn.novelai.api.ImageGenerationPayload;
import com.zxzinn.novelai.service.generation.ImageGenerationService;
import com.zxzinn.novelai.utils.common.NAIConstants;
import com.zxzinn.novelai.utils.common.SettingsManager;
import com.zxzinn.novelai.utils.embed.EmbedProcessor;
import com.zxzinn.novelai.utils.image.ImageProcessor;
import com.zxzinn.novelai.utils.image.ImageUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import lombok.extern.log4j.Log4j2;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Log4j2
public abstract class AbstractGenerationController {
    protected final APIClient apiClient;
    protected final EmbedProcessor embedProcessor;
    protected final SettingsManager settingsManager;
    protected final ImageGenerationService imageGenerationService;
    protected final ImageUtils imageUtils;

    @FXML protected TextField apiKeyField;
    @FXML protected ComboBox<String> modelComboBox;
    @FXML protected TextField widthField;
    @FXML protected TextField heightField;
    @FXML protected TextField ratioField;
    @FXML protected TextField countField;
    @FXML protected ComboBox<String> samplerComboBox;
    @FXML protected CheckBox smeaCheckBox;
    @FXML protected CheckBox smeaDynCheckBox;
    @FXML protected TextField stepsField;
    @FXML protected TextField seedField;
    @FXML protected TextArea positivePromptArea;
    @FXML protected TextArea negativePromptArea;
    @FXML protected TextArea positivePromptPreviewArea;
    @FXML protected TextArea negativePromptPreviewArea;
    @FXML protected ComboBox<String> generateCountComboBox;
    @FXML protected TextField watermarkTextField;
    @FXML protected CheckBox clearLSBCheckBox;
    @FXML protected Button generateButton;
    @FXML protected ScrollPane mainScrollPane;
    @FXML protected VBox historyImagesContainer;
    @FXML protected ImageView mainImageView;

    protected int currentGeneratedCount = 0;
    protected CountDownLatch promptUpdateLatch;

    public AbstractGenerationController(APIClient apiClient, EmbedProcessor embedProcessor,
                                        SettingsManager settingsManager,
                                        ImageGenerationService imageGenerationService,
                                        ImageUtils imageUtils) {
        this.apiClient = apiClient;
        this.embedProcessor = embedProcessor;
        this.settingsManager = settingsManager;
        this.imageGenerationService = imageGenerationService;
        this.imageUtils = imageUtils;
    }

    @FXML
    public void initialize() {
        initializeFields();
        loadSettings();
        setupListeners();
        setupZoomHandler();
        updatePromptPreviews();
    }

    protected void initializeFields() {
        if (apiKeyField != null) apiKeyField.setText("");

        if (modelComboBox != null) {
            modelComboBox.setItems(FXCollections.observableArrayList(NAIConstants.MODELS));
            modelComboBox.setValue(NAIConstants.MODELS[0]);
        }

        if (widthField != null) widthField.setText("832");
        if (heightField != null) heightField.setText("1216");
        if (ratioField != null) ratioField.setText("7");
        if (countField != null) countField.setText("1");

        if (samplerComboBox != null) {
            samplerComboBox.setItems(FXCollections.observableArrayList(NAIConstants.SAMPLERS));
            samplerComboBox.setValue(NAIConstants.SAMPLERS[0]);
        }

        if (stepsField != null) stepsField.setText("28");
        if (seedField != null) seedField.setText("0");

        if (generateCountComboBox != null) {
            generateCountComboBox.getItems().addAll("1", "2", "3", "4", "無限");
            generateCountComboBox.setValue("1");
        }
        if (watermarkTextField != null) watermarkTextField.setText("");
        if (clearLSBCheckBox != null) clearLSBCheckBox.setSelected(false);
    }

    protected void setupZoomHandler() {
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
    protected void handleGenerate() {
        generateButton.setDisable(true);
        currentGeneratedCount = 0;
        promptUpdateLatch = new CountDownLatch(1);
        generateImages();
    }

    protected abstract void generateImages();

    protected void handleGeneratedImage(BufferedImage originalImage) {
        Platform.runLater(() -> {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String timeStamp = now.format(formatter);

            BufferedImage processedImage = processImage(originalImage);

            Image fxImage = SwingFXUtils.toFXImage(processedImage, null);
            mainImageView.setImage(fxImage);
            mainImageView.setPreserveRatio(true);
            mainImageView.setSmooth(true);
            mainImageView.fitWidthProperty().bind(((AnchorPane) mainImageView.getParent()).widthProperty());
            mainImageView.fitHeightProperty().bind(((AnchorPane) mainImageView.getParent()).heightProperty());

            addImageToHistory(fxImage);

            try {
                String fileName = "generated_image_" + timeStamp.replace(":", "-") + "_" + (currentGeneratedCount) + ".png";
                ImageProcessor.saveImage(processedImage, new File("output", fileName));
            } catch (IOException e) {
                log.error("保存圖像時發生錯誤：" + e.getMessage(), e);
            }
        });
    }

    protected BufferedImage processImage(BufferedImage image) {
        if (!watermarkTextField.getText().isEmpty()) {
            ImageProcessor.addWatermark(image, watermarkTextField.getText());
        }

        if (clearLSBCheckBox.isSelected()) {
            ImageProcessor.clearMetadata(image);
        }

        return image;
    }

    protected void updatePromptPreviewsAsync() {
        Platform.runLater(() -> {
            positivePromptPreviewArea.setText(embedProcessor.processPrompt(positivePromptArea.getText()));
            negativePromptPreviewArea.setText(embedProcessor.processPrompt(negativePromptArea.getText()));
            promptUpdateLatch.countDown();
        });
    }

    protected ImageGenerationPayload createImageGenerationPayload(String processedPositivePrompt, String processedNegativePrompt) {
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

    protected void addImageToHistory(Image image) {
        double aspectRatio = image.getWidth() / image.getHeight();
        ImageView historyImageView = new ImageView(image);
        historyImageView.setPreserveRatio(true);
        historyImageView.setSmooth(true);
        historyImageView.setFitWidth(150);
        historyImageView.setFitHeight(150 / aspectRatio);

        historyImageView.setOnMouseClicked(event -> mainImageView.setImage(image));

        historyImagesContainer.getChildren().addFirst(historyImageView);
    }

    protected void loadSettings() {
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

    protected void setupListeners() {
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

    protected void updatePromptPreview(String newValue, TextArea previewArea) {
        String processedPrompt = embedProcessor.processPrompt(newValue);
        previewArea.setText(processedPrompt);
    }

    protected void updatePromptPreviews() {
        updatePromptPreview(positivePromptArea.getText(), positivePromptPreviewArea);
        updatePromptPreview(negativePromptArea.getText(), negativePromptPreviewArea);
    }
}