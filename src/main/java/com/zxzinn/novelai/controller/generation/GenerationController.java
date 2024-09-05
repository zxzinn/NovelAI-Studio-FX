package com.zxzinn.novelai.controller.generation;

import com.google.inject.Inject;
import com.zxzinn.novelai.api.APIClient;
import com.zxzinn.novelai.api.GenerationPayload;
import com.zxzinn.novelai.component.*;
import com.zxzinn.novelai.service.filemanager.FilePreviewService;
import com.zxzinn.novelai.service.generation.*;
import com.zxzinn.novelai.service.ui.NotificationService;
import com.zxzinn.novelai.utils.embed.EmbedFileManager;
import com.zxzinn.novelai.utils.embed.EmbedProcessor;
import com.zxzinn.novelai.utils.image.ImageUtils;
import com.zxzinn.novelai.utils.strategy.ExponentialBackoffRetry;
import com.zxzinn.novelai.utils.strategy.RetryStrategy;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Log4j2
public class GenerationController {
    private static final String GENERATE_BUTTON_CLASS = "generate-button";

    private final FilePreviewService filePreviewService;
    private final GenerationSettingsManager generationSettingsManager;
    private final PromptManager promptManager;
    private final APIClient apiClient;
    private final RetryStrategy retryStrategy;

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
    @FXML private ImagePreviewPane imagePreviewPane;
    @FXML private Button generateButton;
    @FXML private TitledPane txt2imgSettingsPane;
    @FXML private TitledPane img2imgSettingsPane;
    @FXML private CheckBox smeaCheckBox;
    @FXML private CheckBox smeaDynCheckBox;
    @FXML private Slider strengthSlider;
    @FXML private TextField extraNoiseSeedField;
    @FXML private Button uploadImageButton;

    private int currentGeneratedCount = 0;
    private CountDownLatch promptUpdateLatch;
    private volatile boolean isGenerating = false;
    private volatile boolean stopRequested = false;
    private volatile boolean isStopping = false;
    private String base64Image;

    @Inject
    public GenerationController(FilePreviewService filePreviewService,
                                GenerationSettingsManager generationSettingsManager) {
        this.filePreviewService = filePreviewService;
        this.generationSettingsManager = generationSettingsManager;
        this.promptManager = new PromptManager(new EmbedProcessor());
        this.apiClient = new APIClient();
        this.retryStrategy = new ExponentialBackoffRetry();
    }

    @FXML
    public void initialize() {
        EmbedFileManager embedFileManager = new EmbedFileManager();
        embedFileManager.scanEmbedFiles();

        positivePromptArea.setEmbedFileManager(embedFileManager);
        negativePromptArea.setEmbedFileManager(embedFileManager);

        imagePreviewPane = new ImagePreviewPane(filePreviewService);
        previewContainer.getChildren().add(imagePreviewPane);
        historyImagesPane.setOnImageClickHandler(this::handleHistoryImageClick);
        generateButton.getStyleClass().add(GENERATE_BUTTON_CLASS);

        UIInitializer.builder()
                .modelComboBox(modelComboBox)
                .samplerComboBox(samplerComboBox)
                .generateCountComboBox(generateCountComboBox)
                .positivePromptArea(positivePromptArea)
                .negativePromptArea(negativePromptArea)
                .positivePromptPreviewArea(positivePromptPreviewArea)
                .negativePromptPreviewArea(negativePromptPreviewArea)
                .strengthSlider(strengthSlider)
                .build()
                .initializeFields();

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
        imagePreviewPane.updatePreview(imageFile);
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
            NotificationService.showNotification("請先上傳一張圖片");
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
                    GenerationPayload payload = createGenerationPayload();

                    Optional<byte[]> generatedImageData = generateImageWithRetry(payload, apiKeyField.getText());

                    generatedImageData.ifPresentOrElse(
                            imageData -> {
                                handleGeneratedImage(imageData);
                                currentGeneratedCount++;
                            },
                            () -> Platform.runLater(() -> NotificationService.showNotification("圖像生成失敗,請稍後重試"))
                    );

                    promptUpdateLatch = new CountDownLatch(1);
                    updatePromptPreviewsAsync();
                }
            } finally {
                finishGeneration();
            }
        });
    }

    private Optional<byte[]> generateImageWithRetry(GenerationPayload payload, String apiKey) {
        return retryStrategy.execute(() -> {
            Optional<byte[]> result = generateImage(payload, apiKey);
            return result.orElseThrow(() -> new RuntimeException("Image generation failed"));
        });
    }

    private Optional<byte[]> generateImage(GenerationPayload payload, String apiKey) {
        try {
            byte[] zipData = apiClient.generateImage(payload, apiKey);
            return Optional.of(extractImageFromZip(zipData));
        } catch (IOException e) {
            log.error("生成圖像時發生錯誤：{}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    private byte[] extractImageFromZip(byte[] zipData) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData))) {
            ZipEntry entry = zis.getNextEntry();
            if (entry == null) {
                throw new IOException("ZIP文件為空");
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            zis.transferTo(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void handleGeneratedImage(byte[] imageData) {
        Platform.runLater(() -> {

            Image image = new Image(new ByteArrayInputStream(imageData));
            saveImageToFile(imageData).ifPresent(imageFile -> {
                imagePreviewPane.updatePreview(imageFile);
                historyImagesPane.addImage(image, imageFile);
                NotificationService.showNotification("圖像生成成功！");
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

    private Optional<File> saveImageToFile(byte[] imageData) {
        return ImageUtils.saveImage(imageData, outputDirectoryField.getText());
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
                BufferedImage image = ImageUtils.loadImage(selectedFile);
                base64Image = ImageUtils.imageToBase64(image);
                imagePreviewPane.updatePreview(selectedFile);
                log.info("圖片已上傳: {}", selectedFile.getName());
                NotificationService.showNotification("圖片上傳成功");
            } catch (IOException e) {
                log.error("上傳圖片時發生錯誤", e);
                NotificationService.showNotification("圖片上傳失敗");
            }
        }
    }
}