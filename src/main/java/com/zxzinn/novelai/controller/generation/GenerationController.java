package com.zxzinn.novelai.controller.generation;

import com.google.inject.Inject;
import com.zxzinn.novelai.api.GenerationPayload;
import com.zxzinn.novelai.component.*;
import com.zxzinn.novelai.service.filemanager.FilePreviewService;
import com.zxzinn.novelai.service.generation.*;
import com.zxzinn.novelai.service.ui.NotificationService;
import com.zxzinn.novelai.utils.embed.EmbedFileManager;
import com.zxzinn.novelai.utils.embed.EmbedProcessor;
import com.zxzinn.novelai.utils.image.ImageUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Log4j2
public class GenerationController {
    private static final int MAX_RETRIES = 5;
    private static final long RETRY_DELAY = 20000;
    private static final String GENERATE_BUTTON_CLASS = "generate-button";

    private final EmbedProcessor embedProcessor;
    private final ImageGenerationService imageGenerationService;
    private final ImageUtils imageUtils;
    private final FilePreviewService filePreviewService;
    private final GenerationSettingsManager generationSettingsManager;
    private final UIInitializer uiInitializer;
    private final GenerationHandler generationHandler;
    private final PromptManager promptManager;

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
    @FXML private StackPane previewContainer;
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
    private String base64Image;

    @Inject
    public GenerationController(EmbedProcessor embedProcessor,
                                ImageGenerationService imageGenerationService,
                                ImageUtils imageUtils,
                                FilePreviewService filePreviewService,
                                GenerationSettingsManager generationSettingsManager) {
        this.embedProcessor = embedProcessor;
        this.imageGenerationService = imageGenerationService;
        this.imageUtils = imageUtils;
        this.filePreviewService = filePreviewService;
        this.generationSettingsManager = generationSettingsManager;
        this.uiInitializer = new UIInitializer();
        this.generationHandler = new GenerationHandler(imageGenerationService);
        this.promptManager = new PromptManager(embedProcessor);
    }

    @FXML
    public void initialize() {
        EmbedFileManager embedFileManager = new EmbedFileManager();
        embedFileManager.scanEmbedFiles();

        positivePromptArea.setEmbedFileManager(embedFileManager);
        negativePromptArea.setEmbedFileManager(embedFileManager);

        previewPane = new PreviewPane(filePreviewService);
        previewContainer.getChildren().add(previewPane);
        historyImagesPane.setOnImageClickHandler(this::handleHistoryImageClick);
        generateButton.getStyleClass().add(GENERATE_BUTTON_CLASS);

        UIInitializer.initializeFields(modelComboBox, samplerComboBox, generateCountComboBox,
                positivePromptArea, negativePromptArea, positivePromptPreviewArea, negativePromptPreviewArea, strengthSlider);
        loadSettings();
        setupListeners();
        promptManager.setupPromptControls(positivePromptControls, negativePromptControls,
                positivePromptArea, negativePromptArea, positivePromptPreviewArea, negativePromptPreviewArea);
        updatePromptPreviews();
        setupGenerationModeComboBox();
        UIInitializer.setupVerticalLayout(modelComboBox, generationModeComboBox, samplerComboBox, generateCountComboBox);
    }

    private void loadSettings() {
        SettingsBuilder.builder()
                .apiKeyField(apiKeyField)
                .modelComboBox(modelComboBox)
                .widthField(widthField)
                .heightField(heightField)
                .samplerComboBox(samplerComboBox)
                .stepsField(stepsField)
                .seedField(seedField)
                .generateCountComboBox(generateCountComboBox)
                .positivePromptArea(positivePromptArea)
                .negativePromptArea(negativePromptArea)
                .outputDirectoryField(outputDirectoryField)
                .generationModeComboBox(generationModeComboBox)
                .smeaCheckBox(smeaCheckBox)
                .smeaDynCheckBox(smeaDynCheckBox)
                .strengthSlider(strengthSlider)
                .extraNoiseSeedField(extraNoiseSeedField)
                .ratioField(ratioField)
                .countField(countField)
                .build()
                .loadSettings(generationSettingsManager);
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

    private void handleHistoryImageClick(File imageFile) {
        previewPane.updatePreview(imageFile);
    }

    private void updatePromptPreviewsAsync() {
        Platform.runLater(() -> {
            promptManager.refreshPromptPreview(positivePromptArea, positivePromptPreviewArea, true);
            promptManager.refreshPromptPreview(negativePromptArea, negativePromptPreviewArea, false);
            promptUpdateLatch.countDown();
        });
    }

    private void setupListeners() {
        ListenersBuilder.builder()
                .apiKeyField(apiKeyField)
                .modelComboBox(modelComboBox)
                .widthField(widthField)
                .heightField(heightField)
                .samplerComboBox(samplerComboBox)
                .stepsField(stepsField)
                .seedField(seedField)
                .generateCountComboBox(generateCountComboBox)
                .positivePromptArea(positivePromptArea)
                .negativePromptArea(negativePromptArea)
                .outputDirectoryField(outputDirectoryField)
                .generationModeComboBox(generationModeComboBox)
                .smeaCheckBox(smeaCheckBox)
                .smeaDynCheckBox(smeaDynCheckBox)
                .strengthSlider(strengthSlider)
                .extraNoiseSeedField(extraNoiseSeedField)
                .ratioField(ratioField)
                .countField(countField)
                .build()
                .setupListeners(generationSettingsManager);


        positivePromptArea.getPromptTextArea().textProperty().addListener((observable, oldValue, newValue) ->
                promptManager.updatePromptPreview(newValue, positivePromptPreviewArea, true));
        negativePromptArea.getPromptTextArea().textProperty().addListener((observable, oldValue, newValue) ->
                promptManager.updatePromptPreview(newValue, negativePromptPreviewArea, false));
    }

    private void updatePromptPreviews() {
        promptManager.refreshPromptPreview(positivePromptArea, positivePromptPreviewArea, true);
        promptManager.refreshPromptPreview(negativePromptArea, negativePromptPreviewArea, false);
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
            generateButton.getStyleClass().removeAll("generate-button", "generate-button-generating", "generate-button-stop");
            if (generating) {
                generateButton.getStyleClass().add("generate-button-generating");
                generateButton.setText("停止");
            } else if (isStopping) {
                generateButton.getStyleClass().add("generate-button-stop");
                generateButton.setText("停止中...");
            } else {
                generateButton.getStyleClass().add("generate-button");
                generateButton.setText("生成");
            }
            generateButton.setDisable(isStopping);
        });
    }

    private void generateImages() {
        CompletableFuture.runAsync(() -> {
            try {
                int maxCount = getMaxCount();
                while (isGenerating && !stopRequested && currentGeneratedCount < maxCount) {
                    if (!promptUpdateLatch.await(5, TimeUnit.SECONDS)) {
                        log.warn("等待提示詞更新超時");
                    }

                    GenerationPayload payload = createGenerationPayload();
                    Optional<byte[]> generatedImageData = generateImageWithRetry(payload);

                    generatedImageData.ifPresentOrElse(
                            imageData -> {
                                handleGeneratedImage(imageData);
                                currentGeneratedCount++;
                            },
                            () -> Platform.runLater(() -> NotificationService.showNotification("圖像生成失敗,請稍後重試", Duration.seconds(5)))
                    );

                    // 更新提示詞預覽，即使是最後一次生成
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

    private Optional<byte[]> generateImageWithRetry(GenerationPayload payload) {
        return generationHandler.generateImageWithRetry(payload, apiKeyField.getText());
    }

    private void handleGeneratedImage(byte[] imageData) {
        Platform.runLater(() -> {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String timeStamp = now.format(formatter);

            Image fxImage = new Image(new ByteArrayInputStream(imageData));
            saveImageToFile(imageData, timeStamp).ifPresent(imageFile -> {
                previewPane.updatePreview(imageFile);
                addImageToHistory(fxImage, imageFile);
                NotificationService.showNotification("圖像生成成功！", Duration.seconds(3));
            });

            // 根據鎖定狀態更新提示詞預覽
            if (!promptManager.isPositivePromptLocked()) {
                promptManager.refreshPromptPreview(positivePromptArea, positivePromptPreviewArea, true);
            }
            if (!promptManager.isNegativePromptLocked()) {
                promptManager.refreshPromptPreview(negativePromptArea, negativePromptPreviewArea, false);
            }
        });
    }

    private Optional<File> saveImageToFile(byte[] imageData, String timeStamp) {
        return generationHandler.saveImageToFile(imageData, outputDirectoryField.getText(), timeStamp, currentGeneratedCount);
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
        return PayloadBuilder.builder()
                .generationMode(generationModeComboBox.getValue())
                .processedPositivePrompt(positivePromptPreviewArea.getPreviewText())
                .processedNegativePrompt(negativePromptPreviewArea.getPreviewText())
                .model(modelComboBox.getValue())
                .width(Integer.parseInt(widthField.getText()))
                .height(Integer.parseInt(heightField.getText()))
                .scale(Integer.parseInt(ratioField.getText()))
                .sampler(samplerComboBox.getValue())
                .steps(Integer.parseInt(stepsField.getText()))
                .nSamples(Integer.parseInt(countField.getText()))
                .seed(Long.parseLong(seedField.getText()))
                .smea(smeaCheckBox.isSelected())
                .smeaDyn(smeaDynCheckBox.isSelected())
                .base64Image(base64Image)
                .strength(strengthSlider.getValue())
                .extraNoiseSeed(Long.parseLong(extraNoiseSeedField.getText()))
                .build()
                .createPayload();
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