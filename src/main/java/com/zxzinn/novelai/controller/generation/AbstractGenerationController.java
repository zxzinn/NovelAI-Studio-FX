package com.zxzinn.novelai.controller.generation;

import com.zxzinn.novelai.api.APIClient;
import com.zxzinn.novelai.api.GenerationPayload;

import com.zxzinn.novelai.component.HistoryImagesPane;
import com.zxzinn.novelai.component.ImageControlBar;
import com.zxzinn.novelai.component.PreviewPane;
import com.zxzinn.novelai.service.filemanager.FilePreviewService;
import com.zxzinn.novelai.service.generation.ImageGenerationService;
import com.zxzinn.novelai.utils.common.NAIConstants;
import com.zxzinn.novelai.utils.common.SettingsManager;
import com.zxzinn.novelai.utils.embed.EmbedProcessor;
import com.zxzinn.novelai.utils.image.ImageProcessor;
import com.zxzinn.novelai.utils.image.ImageUtils;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.extern.log4j.Log4j2;
import org.kordamp.ikonli.javafx.FontIcon;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CountDownLatch;

@Log4j2
public abstract class AbstractGenerationController {
    protected final APIClient apiClient;
    protected final EmbedProcessor embedProcessor;
    protected final SettingsManager settingsManager;
    protected final ImageGenerationService imageGenerationService;
    protected final ImageUtils imageUtils;
    protected final FilePreviewService filePreviewService;

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
    @FXML protected VBox historyImagesContainer;
    @FXML protected StackPane previewContainer;
    @FXML private ImageControlBar imageControlBar;
    @FXML private HistoryImagesPane historyImagesPane;
    @FXML protected TextField outputDirectoryField;
    @FXML protected PreviewPane previewPane;

    protected int currentGeneratedCount = 0;
    protected CountDownLatch promptUpdateLatch;

    @FXML protected Button generateButton;
    @FXML protected Button refreshPositivePromptButton;
    @FXML protected Button refreshNegativePromptButton;
    @FXML protected Button lockPositivePromptButton;
    @FXML protected Button lockNegativePromptButton;
    @FXML protected FontIcon lockPositivePromptIcon;
    @FXML protected FontIcon lockNegativePromptIcon;

    protected boolean isPositivePromptLocked = false;
    protected boolean isNegativePromptLocked = false;

    protected volatile boolean isGenerating = false;
    protected volatile boolean stopRequested = false;
    protected volatile boolean isStopping = false;

    protected static final int MAX_RETRIES = 5;
    protected static final long RETRY_DELAY = 20000;

    public AbstractGenerationController(APIClient apiClient, EmbedProcessor embedProcessor,
                                        SettingsManager settingsManager,
                                        ImageGenerationService imageGenerationService,
                                        ImageUtils imageUtils,
                                        FilePreviewService filePreviewService) {
        this.apiClient = apiClient;
        this.embedProcessor = embedProcessor;
        this.settingsManager = settingsManager;
        this.imageGenerationService = imageGenerationService;
        this.imageUtils = imageUtils;
        this.filePreviewService = filePreviewService;
    }

    @FXML
    public void initialize() {
        previewPane = new PreviewPane(filePreviewService);
        previewContainer.getChildren().add(previewPane);
        historyImagesPane.setOnImageClickHandler(this::handleHistoryImageClick);
        initializeFields();
        loadSettings();
        setupListeners();
        setupRefreshButtons();
        setupTextAreas();
        updatePromptPreviews();
        setupLockButtons();
    }

    private void setupLockButtons() {
        lockPositivePromptButton.setOnAction(event -> toggleLock(true));
        lockNegativePromptButton.setOnAction(event -> toggleLock(false));
    }

    private void toggleLock(boolean isPositive) {
        if (isPositive) {
            isPositivePromptLocked = !isPositivePromptLocked;
            updateLockIcon(lockPositivePromptIcon, isPositivePromptLocked);
        } else {
            isNegativePromptLocked = !isNegativePromptLocked;
            updateLockIcon(lockNegativePromptIcon, isNegativePromptLocked);
        }
    }

    protected void handleHistoryImageClick(File imageFile) {
        previewPane.updatePreview(imageFile);
    }

    private void updateLockIcon(FontIcon icon, boolean isLocked) {
        icon.setIconLiteral(isLocked ? "fas-lock" : "fas-lock-open");
    }

    protected void updatePromptPreviewsAsync() {
        Platform.runLater(() -> {
            if (!isPositivePromptLocked) {
                positivePromptPreviewArea.setText(embedProcessor.processPrompt(positivePromptArea.getText()));
            }
            if (!isNegativePromptLocked) {
                negativePromptPreviewArea.setText(embedProcessor.processPrompt(negativePromptArea.getText()));
            }
            promptUpdateLatch.countDown();
        });
    }

    private void setupTextAreas() {
        setupTextArea(positivePromptArea);
        setupTextArea(negativePromptArea);
        setupTextArea(positivePromptPreviewArea);
        setupTextArea(negativePromptPreviewArea);
    }

    private void setupTextArea(TextArea textArea) {
        textArea.setWrapText(true);
        textArea.setMinHeight(100);
        textArea.setPrefRowCount(5);
        textArea.setMaxHeight(Double.MAX_VALUE);
    }

    private void setupRefreshButtons() {
        refreshPositivePromptButton.setOnAction(event -> refreshPromptPreview(positivePromptArea, positivePromptPreviewArea));
        refreshNegativePromptButton.setOnAction(event -> refreshPromptPreview(negativePromptArea, negativePromptPreviewArea));
    }

    private void refreshPromptPreview(TextArea promptArea, TextArea previewArea) {
        String processedPrompt = embedProcessor.processPrompt(promptArea.getText());
        previewArea.setText(processedPrompt);
    }

    protected void initializeFields() {
        modelComboBox.setItems(FXCollections.observableArrayList(NAIConstants.MODELS));
        modelComboBox.setValue(NAIConstants.MODELS[0]);

        samplerComboBox.setItems(FXCollections.observableArrayList(NAIConstants.SAMPLERS));
        samplerComboBox.setValue(NAIConstants.SAMPLERS[0]);

        generateCountComboBox.getItems().addAll("1", "2", "3", "4", "無限");
        generateCountComboBox.setValue("1");

        widthField.setText("832");
        heightField.setText("1216");
        ratioField.setText("7");
        countField.setText("1");
        stepsField.setText("28");
        seedField.setText("0");
        outputDirectoryField.setText("output");
    }

    @FXML
    protected void handleGenerateOrStop() {
        if (!isGenerating && !isStopping) {
            startGeneration();
        } else if (isGenerating && !isStopping) {
            stopGeneration();
        }
    }

    protected void startGeneration() {
        isGenerating = true;
        stopRequested = false;
        isStopping = false;
        currentGeneratedCount = 0;
        promptUpdateLatch = new CountDownLatch(1);
        updateButtonState(true);
        generateImages();
    }

    protected void stopGeneration() {
        stopRequested = true;
        isStopping = true;
        updateButtonState(false);
    }

    protected void updateButtonState(boolean generating) {
        Platform.runLater(() -> {
            if (generating) {
                generateButton.setText("停止");
                generateButton.setStyle("-fx-background-color: #e53e3e;"); // 紅色
            } else {
                generateButton.setText("生成");
                generateButton.setStyle("-fx-background-color: #48bb78;"); // 綠色
                generateButton.setDisable(isStopping);
            }

            // 添加按鈕動畫
            ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(100), generateButton);
            scaleTransition.setToX(1.1);
            scaleTransition.setToY(1.1);
            scaleTransition.setCycleCount(2);
            scaleTransition.setAutoReverse(true);
            scaleTransition.play();
        });
    }

    protected abstract void generateImages();

    protected void finishGeneration() {
        isGenerating = false;
        isStopping = false;
        updateButtonState(false);
    }

    protected abstract GenerationPayload createGenerationPayload(String processedPositivePrompt, String processedNegativePrompt);

    protected void handleGeneratedImage(BufferedImage originalImage) {
        Platform.runLater(() -> {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String timeStamp = now.format(formatter);

            Image fxImage = imageUtils.convertToFxImage(originalImage);
            File imageFile = saveImageToFile(originalImage, timeStamp);
            if (imageFile != null) {
                previewPane.updatePreview(imageFile);
                addImageToHistory(fxImage, imageFile);
            }
        });
    }

    private File saveImageToFile(BufferedImage image, String timeStamp) {
        try {
            String outputDir = outputDirectoryField.getText();
            Path outputPath = Paths.get(outputDir);
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
            }

            String fileName = "generated_image_" + timeStamp.replace(":", "-") + "_" + (currentGeneratedCount) + ".png";
            File outputFile = outputPath.resolve(fileName).toFile();
            ImageProcessor.saveImage(image, outputFile);
            return outputFile;
        } catch (IOException e) {
            log.error("保存圖像時發生錯誤：" + e.getMessage(), e);
            return null;
        }
    }

    protected void addImageToHistory(Image image, File imageFile) {
        historyImagesPane.addImage(image, imageFile);
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
        outputDirectoryField.setText(settingsManager.getString("outputDirectory", "output"));
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
        outputDirectoryField.textProperty().addListener((obs, oldVal, newVal) ->
                settingsManager.setString("outputDirectory", newVal));

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