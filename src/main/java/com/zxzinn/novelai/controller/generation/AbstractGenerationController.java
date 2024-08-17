package com.zxzinn.novelai.controller.generation;

import com.zxzinn.novelai.api.APIClient;
import com.zxzinn.novelai.api.GenerationPayload;
import com.zxzinn.novelai.component.HistoryImagesPane;
import com.zxzinn.novelai.component.ImageControlBar;
import com.zxzinn.novelai.component.PreviewPane;
import com.zxzinn.novelai.service.filemanager.FilePreviewService;
import com.zxzinn.novelai.service.generation.ImageGenerationService;
import com.zxzinn.novelai.service.ui.NotificationService;
import com.zxzinn.novelai.utils.common.NAIConstants;
import com.zxzinn.novelai.utils.common.SettingsManager;
import com.zxzinn.novelai.utils.common.UIUtils;
import com.zxzinn.novelai.utils.embed.EmbedProcessor;
import com.zxzinn.novelai.utils.image.ImageProcessor;
import com.zxzinn.novelai.utils.image.ImageUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

@Log4j2
@RequiredArgsConstructor
public abstract class AbstractGenerationController {
    private static final int MAX_RETRIES = 5;
    private static final long RETRY_DELAY = 20000;
    private static final String DEFAULT_WIDTH = "832";
    private static final String DEFAULT_HEIGHT = "1216";
    private static final String DEFAULT_RATIO = "7";
    private static final String DEFAULT_COUNT = "1";
    private static final String DEFAULT_STEPS = "28";
    private static final String DEFAULT_SEED = "0";
    private static final String DEFAULT_OUTPUT_DIRECTORY = "output";

    private final APIClient apiClient;
    private final EmbedProcessor embedProcessor;
    protected final SettingsManager settingsManager;
    private final ImageGenerationService imageGenerationService;
    protected final ImageUtils imageUtils;
    private final FilePreviewService filePreviewService;

    @FXML protected TextField apiKeyField;
    @FXML protected ComboBox<String> modelComboBox;
    @FXML protected TextField widthField;
    @FXML protected TextField heightField;
    @FXML protected TextField ratioField;
    @FXML protected TextField countField;
    @FXML protected ComboBox<String> samplerComboBox;
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
    @FXML protected Button generateButton;
    @FXML protected Button refreshPositivePromptButton;
    @FXML protected Button refreshNegativePromptButton;
    @FXML protected Button lockPositivePromptButton;
    @FXML protected Button lockNegativePromptButton;
    @FXML protected FontIcon lockPositivePromptIcon;
    @FXML protected FontIcon lockNegativePromptIcon;

    @Getter protected int currentGeneratedCount = 0;
    protected CountDownLatch promptUpdateLatch;
    protected volatile boolean isGenerating = false;
    protected volatile boolean stopRequested = false;
    protected volatile boolean isStopping = false;
    protected boolean isPositivePromptLocked = false;
    protected boolean isNegativePromptLocked = false;

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

        widthField.setText(DEFAULT_WIDTH);
        heightField.setText(DEFAULT_HEIGHT);
        ratioField.setText(DEFAULT_RATIO);
        countField.setText(DEFAULT_COUNT);
        stepsField.setText(DEFAULT_STEPS);
        seedField.setText(DEFAULT_SEED);
        outputDirectoryField.setText(DEFAULT_OUTPUT_DIRECTORY);
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
            generateButton.setText(generating ? "停止" : "生成");
            generateButton.setStyle(generating ? "-fx-background-color: #e53e3e;" : "-fx-background-color: #48bb78;");
            generateButton.setDisable(isStopping);
            UIUtils.animateButton(generateButton);
        });
    }

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

                    Optional<BufferedImage> generatedImage = generateImageWithRetry(payload);

                    generatedImage.ifPresent(image -> {
                        handleGeneratedImage(image);
                        currentGeneratedCount++;
                        Platform.runLater(() -> NotificationService.showNotification("圖像生成成功！", Duration.seconds(3)));
                    });

                    if (!generatedImage.isPresent()) {
                        Platform.runLater(() -> NotificationService.showNotification("圖像生成失敗,請稍後重試", Duration.seconds(5)));
                    }

                    promptUpdateLatch = new CountDownLatch(1);
                    updatePromptPreviewsAsync();
                }
            } catch (InterruptedException e) {
                log.warn("圖像生成過程被中斷");
                Thread.currentThread().interrupt();
            } finally {
                finishGeneration();
            }
        });
    }

    private Optional<BufferedImage> generateImageWithRetry(GenerationPayload payload) {
        for (int retry = 0; retry < MAX_RETRIES; retry++) {
            try {
                return Optional.of(imageGenerationService.generateImage(payload, apiKeyField.getText()));
            } catch (IOException e) {
                if (retry == MAX_RETRIES - 1) {
                    log.error("生成圖像失敗，已達到最大重試次數", e);
                    return Optional.empty();
                }
                log.warn("生成圖像失敗,將在{}毫秒後重試. 錯誤: {}", RETRY_DELAY, e.getMessage());
                try {
                    Thread.sleep(RETRY_DELAY);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    protected int getMaxCount() {
        String selectedCount = generateCountComboBox.getValue();
        return "無限".equals(selectedCount) ? Integer.MAX_VALUE : Integer.parseInt(selectedCount);
    }

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
            saveImageToFile(originalImage, timeStamp).ifPresent(imageFile -> {
                previewPane.updatePreview(imageFile);
                addImageToHistory(fxImage, imageFile);
            });
        });
    }

    private Optional<File> saveImageToFile(BufferedImage image, String timeStamp) {
        try {
            String outputDir = outputDirectoryField.getText();
            Path outputPath = Paths.get(outputDir);
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
            }

            String fileName = String.format("generated_image_%s_%d.png", timeStamp.replace(":", "-"), currentGeneratedCount);
            File outputFile = outputPath.resolve(fileName).toFile();
            ImageProcessor.saveImage(image, outputFile);
            return Optional.of(outputFile);
        } catch (IOException e) {
            log.error("保存圖像時發生錯誤：" + e.getMessage(), e);
            return Optional.empty();
        }
    }

    protected void addImageToHistory(Image image, File imageFile) {
        historyImagesPane.addImage(image, imageFile);
    }

    protected void loadSettings() {
        apiKeyField.setText(settingsManager.getString("apiKey", ""));
        modelComboBox.setValue(settingsManager.getString("model", "nai-diffusion-3"));
        widthField.setText(String.valueOf(settingsManager.getInt("width", Integer.parseInt(DEFAULT_WIDTH))));
        heightField.setText(String.valueOf(settingsManager.getInt("height", Integer.parseInt(DEFAULT_HEIGHT))));
        samplerComboBox.setValue(settingsManager.getString("sampler", "k_euler"));
        stepsField.setText(String.valueOf(settingsManager.getInt("steps", Integer.parseInt(DEFAULT_STEPS))));
        seedField.setText(String.valueOf(settingsManager.getInt("seed", Integer.parseInt(DEFAULT_SEED))));
        generateCountComboBox.setValue(settingsManager.getString("generateCount", "1"));
        positivePromptArea.setText(settingsManager.getString("positivePrompt", ""));
        negativePromptArea.setText(settingsManager.getString("negativePrompt", ""));
        outputDirectoryField.setText(settingsManager.getString("outputDirectory", DEFAULT_OUTPUT_DIRECTORY));
    }

    protected void setupListeners() {
        setupTextFieldListener(apiKeyField, "apiKey", settingsManager::setString);
        setupComboBoxListener(modelComboBox, "model", settingsManager::setString);
        setupTextFieldListener(widthField, "width", (key, value) -> settingsManager.setInt(key, Integer.parseInt(value)));
        setupTextFieldListener(heightField, "height", (key, value) -> settingsManager.setInt(key, Integer.parseInt(value)));
        setupComboBoxListener(samplerComboBox, "sampler", settingsManager::setString);
        setupTextFieldListener(stepsField, "steps", (key, value) -> settingsManager.setInt(key, Integer.parseInt(value)));
        setupTextFieldListener(seedField, "seed", (key, value) -> settingsManager.setInt(key, Integer.parseInt(value)));
        setupComboBoxListener(generateCountComboBox, "generateCount", settingsManager::setString);
        setupTextAreaListener(positivePromptArea, "positivePrompt", settingsManager::setString);
        setupTextAreaListener(negativePromptArea, "negativePrompt", settingsManager::setString);
        setupTextFieldListener(outputDirectoryField, "outputDirectory", settingsManager::setString);

        positivePromptArea.textProperty().addListener((observable, oldValue, newValue) ->
                updatePromptPreview(newValue, positivePromptPreviewArea));
        negativePromptArea.textProperty().addListener((observable, oldValue, newValue) ->
                updatePromptPreview(newValue, negativePromptPreviewArea));
    }

    private void setupTextFieldListener(TextField textField, String key, BiConsumer<String, String> setter) {
        textField.textProperty().addListener((obs, oldVal, newVal) -> setter.accept(key, newVal));
    }

    private void setupComboBoxListener(ComboBox<String> comboBox, String key, BiConsumer<String, String> setter) {
        comboBox.valueProperty().addListener((obs, oldVal, newVal) -> setter.accept(key, newVal));
    }

    private void setupTextAreaListener(TextArea textArea, String key, BiConsumer<String, String> setter) {
        textArea.textProperty().addListener((obs, oldVal, newVal) -> setter.accept(key, newVal));
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