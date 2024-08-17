package com.zxzinn.novelai.controller.generation;

import com.zxzinn.novelai.api.APIClient;
import com.zxzinn.novelai.api.GenerationPayload;
import com.zxzinn.novelai.component.*;
import com.zxzinn.novelai.service.filemanager.FilePreviewService;
import com.zxzinn.novelai.service.generation.GenerationPayloadFactory;
import com.zxzinn.novelai.service.generation.GenerationSettingsManager;
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
import javafx.stage.FileChooser;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

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

@Log4j2
@RequiredArgsConstructor
public class UnifiedGeneratorController {
    private static final int MAX_RETRIES = 5;
    private static final long RETRY_DELAY = 20000;

    private final APIClient apiClient;
    private final EmbedProcessor embedProcessor;
    private final SettingsManager settingsManager;
    private final ImageGenerationService imageGenerationService;
    private final ImageUtils imageUtils;
    private final FilePreviewService filePreviewService;
    private final GenerationSettingsManager generationSettingsManager;

    @FXML private TextField apiKeyField;
    @FXML private ComboBox<String> modelComboBox;
    @FXML private ComboBox<String> generationModeComboBox;
    @FXML private TextField widthField;
    @FXML private TextField heightField;
    @FXML private TextField ratioField;
    @FXML private TextField countField;
    @FXML private ComboBox<String> samplerComboBox;
    @FXML private TextField stepsField;
    @FXML private TextField seedField;
    @FXML private PromptArea positivePromptArea;
    @FXML private PromptArea negativePromptArea;
    @FXML private PromptPreviewArea positivePromptPreviewArea;
    @FXML private PromptPreviewArea negativePromptPreviewArea;
    @FXML private PromptControls positivePromptControls;
    @FXML private PromptControls negativePromptControls;
    @FXML private ComboBox<String> generateCountComboBox;
    @FXML private VBox historyImagesContainer;
    @FXML private StackPane previewContainer;
    @FXML private ImageControlBar imageControlBar;
    @FXML private HistoryImagesPane historyImagesPane;
    @FXML private TextField outputDirectoryField;
    @FXML private PreviewPane previewPane;
    @FXML private Button generateButton;
    @FXML private TitledPane txt2imgSettingsPane;
    @FXML private TitledPane img2imgSettingsPane;
    @FXML private CheckBox smeaCheckBox;
    @FXML private CheckBox smeaDynCheckBox;
    @FXML private Slider strengthSlider;
    @FXML private Label strengthLabel;
    @FXML private TextField extraNoiseSeedField;
    @FXML private Button uploadImageButton;

    private int currentGeneratedCount = 0;
    private CountDownLatch promptUpdateLatch;
    private volatile boolean isGenerating = false;
    private volatile boolean stopRequested = false;
    private volatile boolean isStopping = false;
    private boolean isPositivePromptLocked = false;
    private boolean isNegativePromptLocked = false;
    private String base64Image;

    @FXML
    public void initialize() {
        previewPane = new PreviewPane(filePreviewService);
        previewContainer.getChildren().add(previewPane);
        historyImagesPane.setOnImageClickHandler(this::handleHistoryImageClick);
        initializeFields();
        generationSettingsManager.loadSettings(apiKeyField, modelComboBox, widthField, heightField, samplerComboBox,
                stepsField, seedField, generateCountComboBox, positivePromptArea, negativePromptArea,
                outputDirectoryField, generationModeComboBox, smeaCheckBox, smeaDynCheckBox, strengthSlider, extraNoiseSeedField);
        setupListeners();
        setupPromptControls();
        updatePromptPreviews();
        setupGenerationModeComboBox();
    }

    private void initializeFields() {
        modelComboBox.setItems(FXCollections.observableArrayList(NAIConstants.MODELS));
        modelComboBox.setValue(NAIConstants.MODELS[0]);
        samplerComboBox.setItems(FXCollections.observableArrayList(NAIConstants.SAMPLERS));
        samplerComboBox.setValue(NAIConstants.SAMPLERS[0]);
        generateCountComboBox.getItems().addAll("1", "2", "3", "4", "無限");
        generateCountComboBox.setValue("1");
        positivePromptArea.setPromptLabel("正面提示詞:");
        negativePromptArea.setPromptLabel("負面提示詞:");
        positivePromptPreviewArea.setPreviewLabel("正面提示詞預覽");
        negativePromptPreviewArea.setPreviewLabel("負面提示詞預覽");
        setupStrengthSlider();
    }

    private void setupStrengthSlider() {
        strengthSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            double roundedValue = Math.round(newValue.doubleValue() * 10.0) / 10.0;
            strengthSlider.setValue(roundedValue);
            strengthLabel.setText(String.format("%.1f", roundedValue));
        });
    }

    private void setupGenerationModeComboBox() {
        generationModeComboBox.getItems().addAll("Text2Image", "Image2Image");
        generationModeComboBox.setValue("Text2Image");
        generationModeComboBox.setOnAction(event -> updateSettingsPanes());
        updateSettingsPanes();
    }

    private void updateSettingsPanes() {
        boolean isText2Image = "Text2Image".equals(generationModeComboBox.getValue());
        txt2imgSettingsPane.setVisible(isText2Image);
        txt2imgSettingsPane.setManaged(isText2Image);
        img2imgSettingsPane.setVisible(!isText2Image);
        img2imgSettingsPane.setManaged(!isText2Image);
    }

    private void setupPromptControls() {
        positivePromptControls.setOnRefreshAction(() -> refreshPromptPreview(positivePromptArea, positivePromptPreviewArea));
        negativePromptControls.setOnRefreshAction(() -> refreshPromptPreview(negativePromptArea, negativePromptPreviewArea));
        positivePromptControls.setOnLockAction(() -> toggleLock(true));
        negativePromptControls.setOnLockAction(() -> toggleLock(false));
    }

    private void toggleLock(boolean isPositive) {
        if (isPositive) {
            isPositivePromptLocked = !isPositivePromptLocked;
            positivePromptControls.setLockIcon(isPositivePromptLocked);
        } else {
            isNegativePromptLocked = !isNegativePromptLocked;
            negativePromptControls.setLockIcon(isNegativePromptLocked);
        }
    }

    private void handleHistoryImageClick(File imageFile) {
        previewPane.updatePreview(imageFile);
    }

    private void updatePromptPreviewsAsync() {
        Platform.runLater(() -> {
            if (!isPositivePromptLocked) {
                positivePromptPreviewArea.setPreviewText(embedProcessor.processPrompt(positivePromptArea.getPromptText()));
            }
            if (!isNegativePromptLocked) {
                negativePromptPreviewArea.setPreviewText(embedProcessor.processPrompt(negativePromptArea.getPromptText()));
            }
            promptUpdateLatch.countDown();
        });
    }

    private void refreshPromptPreview(PromptArea promptArea, PromptPreviewArea previewArea) {
        String processedPrompt = embedProcessor.processPrompt(promptArea.getPromptText());
        previewArea.setPreviewText(processedPrompt);
    }

    private void setupListeners() {
        generationSettingsManager.setupListeners(apiKeyField, modelComboBox, widthField, heightField, samplerComboBox,
                stepsField, seedField, generateCountComboBox, positivePromptArea, negativePromptArea,
                outputDirectoryField, generationModeComboBox, smeaCheckBox, smeaDynCheckBox, strengthSlider, extraNoiseSeedField);

        positivePromptArea.getPromptTextArea().textProperty().addListener((observable, oldValue, newValue) ->
                updatePromptPreview(newValue, positivePromptPreviewArea));
        negativePromptArea.getPromptTextArea().textProperty().addListener((observable, oldValue, newValue) ->
                updatePromptPreview(newValue, negativePromptPreviewArea));
    }

    private void updatePromptPreview(String newValue, PromptPreviewArea previewArea) {
        String processedPrompt = embedProcessor.processPrompt(newValue);
        previewArea.setPreviewText(processedPrompt);
    }

    private void updatePromptPreviews() {
        updatePromptPreview(positivePromptArea.getPromptText(), positivePromptPreviewArea);
        updatePromptPreview(negativePromptArea.getPromptText(), negativePromptPreviewArea);
    }

    @FXML
    private void handleGenerateOrStop() {
        if (!isGenerating && !isStopping) {
            startGeneration();
        } else if (isGenerating && !isStopping) {
            stopGeneration();
        }
    }

    private void startGeneration() {
        if ("Image2Image".equals(generationModeComboBox.getValue()) && base64Image == null) {
            NotificationService.showNotification("請先上傳一張圖片", Duration.seconds(3));
            return;
        }

        isGenerating = true;
        stopRequested = false;
        isStopping = false;
        currentGeneratedCount = 0;
        promptUpdateLatch = new CountDownLatch(1);
        updateButtonState(true);
        generateImages();
    }

    private void stopGeneration() {
        stopRequested = true;
        isStopping = true;
        updateButtonState(false);
    }

    private void updateButtonState(boolean generating) {
        Platform.runLater(() -> {
            generateButton.setText(generating ? "停止" : "生成");
            generateButton.setStyle(generating ? "-fx-background-color: #e53e3e;" : "-fx-background-color: #48bb78;");
            generateButton.setDisable(isStopping);
            UIUtils.animateButton(generateButton);
        });
    }

    private void generateImages() {
        CompletableFuture.runAsync(() -> {
            try {
                while (isGenerating && !stopRequested && currentGeneratedCount < getMaxCount()) {
                    if (!promptUpdateLatch.await(5, TimeUnit.SECONDS)) {
                        log.warn("等待提示詞更新超時");
                    }

                    GenerationPayload payload = createGenerationPayload();
                    Optional<BufferedImage> generatedImage = generateImageWithRetry(payload);

                    generatedImage.ifPresentOrElse(
                            this::handleGeneratedImage,
                            () -> Platform.runLater(() -> NotificationService.showNotification("圖像生成失敗,請稍後重試", Duration.seconds(5)))
                    );

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

    private int getMaxCount() {
        String selectedCount = generateCountComboBox.getValue();
        return "無限".equals(selectedCount) ? Integer.MAX_VALUE : Integer.parseInt(selectedCount);
    }

    private void finishGeneration() {
        isGenerating = false;
        isStopping = false;
        updateButtonState(false);
    }

    private GenerationPayload createGenerationPayload() {
        return GenerationPayloadFactory.createPayload(
                generationModeComboBox.getValue(),
                positivePromptPreviewArea.getPreviewText(),
                negativePromptPreviewArea.getPreviewText(),
                modelComboBox.getValue(),
                Integer.parseInt(widthField.getText()),
                Integer.parseInt(heightField.getText()),
                Integer.parseInt(ratioField.getText()),
                samplerComboBox.getValue(),
                Integer.parseInt(stepsField.getText()),
                Integer.parseInt(countField.getText()),
                Long.parseLong(seedField.getText()),
                smeaCheckBox.isSelected(),
                smeaDynCheckBox.isSelected(),
                base64Image,
                strengthSlider.getValue(),
                Long.parseLong(extraNoiseSeedField.getText())
        );
    }

    private void handleGeneratedImage(BufferedImage originalImage) {
        Platform.runLater(() -> {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String timeStamp = now.format(formatter);

            javafx.scene.image.Image fxImage = imageUtils.convertToFxImage(originalImage);
            saveImageToFile(originalImage, timeStamp).ifPresent(imageFile -> {
                previewPane.updatePreview(imageFile);
                addImageToHistory(fxImage, imageFile);
            });
            currentGeneratedCount++;
            NotificationService.showNotification("圖像生成成功！", Duration.seconds(3));
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

    private void addImageToHistory(Image image, File imageFile) {
        historyImagesPane.addImage(image, imageFile);
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
                NotificationService.showNotification("圖片上傳成功", Duration.seconds(3));
            } catch (IOException e) {
                log.error("上傳圖片時發生錯誤", e);
                NotificationService.showNotification("圖片上傳失敗", Duration.seconds(3));
            }
        }
    }
}