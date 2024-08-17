package com.zxzinn.novelai.controller.generation;

import com.zxzinn.novelai.api.APIClient;
import com.zxzinn.novelai.api.GenerationPayload;
import com.zxzinn.novelai.api.ImageGenerationPayload;
import com.zxzinn.novelai.api.Img2ImgGenerationPayload;
import com.zxzinn.novelai.component.*;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import lombok.Getter;
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
import java.util.function.BiConsumer;

@Log4j2
@RequiredArgsConstructor
public class UnifiedGeneratorController {
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
    private final SettingsManager settingsManager;
    private final ImageGenerationService imageGenerationService;
    private final ImageUtils imageUtils;
    private final FilePreviewService filePreviewService;

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

    @Getter private int currentGeneratedCount = 0;
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
        loadSettings();
        setupListeners();
        setupPromptControls();
        updatePromptPreviews();
        setupGenerationModeComboBox();
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

    private void initializeFields() {
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

        positivePromptArea.setPromptLabel("正面提示詞:");
        negativePromptArea.setPromptLabel("負面提示詞:");
        positivePromptPreviewArea.setPreviewLabel("正面提示詞預覽");
        negativePromptPreviewArea.setPreviewLabel("負面提示詞預覽");

        extraNoiseSeedField.setText("0");
        strengthSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            double roundedValue = Math.round(newValue.doubleValue() * 10.0) / 10.0;
            strengthSlider.setValue(roundedValue);
            strengthLabel.setText(String.format("%.1f", roundedValue));
        });
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

                    GenerationPayload payload = createGenerationPayload(
                            positivePromptPreviewArea.getPreviewText(),
                            negativePromptPreviewArea.getPreviewText()
                    );

                    Optional<BufferedImage> generatedImage = generateImageWithRetry(payload);

                    generatedImage.ifPresent(image -> {
                        handleGeneratedImage(image);
                        currentGeneratedCount++;
                        Platform.runLater(() -> NotificationService.showNotification("圖像生成成功！", Duration.seconds(3)));
                    });

                    if (generatedImage.isEmpty()) {
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

    private int getMaxCount() {
        String selectedCount = generateCountComboBox.getValue();
        return "無限".equals(selectedCount) ? Integer.MAX_VALUE : Integer.parseInt(selectedCount);
    }

    private void finishGeneration() {
        isGenerating = false;
        isStopping = false;
        updateButtonState(false);
    }

    private GenerationPayload createGenerationPayload(String processedPositivePrompt, String processedNegativePrompt) {
        if ("Text2Image".equals(generationModeComboBox.getValue())) {
            return createText2ImagePayload(processedPositivePrompt, processedNegativePrompt);
        } else {
            return createImage2ImagePayload(processedPositivePrompt, processedNegativePrompt);
        }
    }

    private ImageGenerationPayload createText2ImagePayload(String processedPositivePrompt, String processedNegativePrompt) {
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

    private Img2ImgGenerationPayload createImage2ImagePayload(String processedPositivePrompt, String processedNegativePrompt) {
        Img2ImgGenerationPayload payload = new Img2ImgGenerationPayload();
        payload.setInput(processedPositivePrompt);
        payload.setModel(modelComboBox.getValue());
        payload.setAction("img2img");

        Img2ImgGenerationPayload.Img2ImgGenerationParameters parameters = new Img2ImgGenerationPayload.Img2ImgGenerationParameters();
        parameters.setWidth(Integer.parseInt(widthField.getText()));
        parameters.setHeight(Integer.parseInt(heightField.getText()));
        parameters.setScale(Integer.parseInt(ratioField.getText()));
        parameters.setSampler(samplerComboBox.getValue());
        parameters.setSteps(Integer.parseInt(stepsField.getText()));
        parameters.setN_samples(Integer.parseInt(countField.getText()));
        parameters.setUcPreset(false);
        parameters.setQualityToggle(false);
        parameters.setSeed(Long.parseLong(seedField.getText()));
        parameters.setNegative_prompt(processedNegativePrompt);
        parameters.setImage(base64Image);
        parameters.setExtra_noise_seed(Long.parseLong(extraNoiseSeedField.getText()));

        parameters.setStrength(strengthSlider.getValue());
        parameters.setNoise(0);
        parameters.setDynamic_thresholding(false);
        parameters.setControlnet_strength(1.0);
        parameters.setLegacy(false);
        parameters.setAdd_original_image(true);
        parameters.setCfg_rescale(0);
        parameters.setNoise_schedule("native");
        parameters.setLegacy_v3_extend(false);

        payload.setParameters(parameters);
        return payload;
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

    private void addImageToHistory(javafx.scene.image.Image image, File imageFile) {
        historyImagesPane.addImage(image, imageFile);
    }

    private void loadSettings() {
        apiKeyField.setText(settingsManager.getString("apiKey", ""));
        modelComboBox.setValue(settingsManager.getString("model", "nai-diffusion-3"));
        widthField.setText(String.valueOf(settingsManager.getInt("width", Integer.parseInt(DEFAULT_WIDTH))));
        heightField.setText(String.valueOf(settingsManager.getInt("height", Integer.parseInt(DEFAULT_HEIGHT))));
        samplerComboBox.setValue(settingsManager.getString("sampler", "k_euler"));
        stepsField.setText(String.valueOf(settingsManager.getInt("steps", Integer.parseInt(DEFAULT_STEPS))));
        seedField.setText(String.valueOf(settingsManager.getInt("seed", Integer.parseInt(DEFAULT_SEED))));
        generateCountComboBox.setValue(settingsManager.getString("generateCount", "1"));
        positivePromptArea.setPromptText(settingsManager.getString("positivePrompt", ""));
        negativePromptArea.setPromptText(settingsManager.getString("negativePrompt", ""));
        outputDirectoryField.setText(settingsManager.getString("outputDirectory", DEFAULT_OUTPUT_DIRECTORY));
        generationModeComboBox.setValue(settingsManager.getString("generationMode", "Text2Image"));
        smeaCheckBox.setSelected(settingsManager.getBoolean("smea", true));
        smeaDynCheckBox.setSelected(settingsManager.getBoolean("smeaDyn", false));
        strengthSlider.setValue(settingsManager.getDouble("strength", 0.5));
        extraNoiseSeedField.setText(String.valueOf(settingsManager.getLong("extraNoiseSeed", 0)));
    }

    private void setupListeners() {
        setupTextFieldListener(apiKeyField, "apiKey", settingsManager::setString);
        setupComboBoxListener(modelComboBox, "model", settingsManager::setString);
        setupTextFieldListener(widthField, "width", (key, value) -> settingsManager.setInt(key, Integer.parseInt(value)));
        setupTextFieldListener(heightField, "height", (key, value) -> settingsManager.setInt(key, Integer.parseInt(value)));
        setupComboBoxListener(samplerComboBox, "sampler", settingsManager::setString);
        setupTextFieldListener(stepsField, "steps", (key, value) -> settingsManager.setInt(key, Integer.parseInt(value)));
        setupTextFieldListener(seedField, "seed", (key, value) -> settingsManager.setInt(key, Integer.parseInt(value)));
        setupComboBoxListener(generateCountComboBox, "generateCount", settingsManager::setString);
        setupPromptAreaListener(positivePromptArea, "positivePrompt", settingsManager::setString);
        setupPromptAreaListener(negativePromptArea, "negativePrompt", settingsManager::setString);
        setupTextFieldListener(outputDirectoryField, "outputDirectory", settingsManager::setString);
        setupComboBoxListener(generationModeComboBox, "generationMode", settingsManager::setString);
        setupCheckBoxListener(smeaCheckBox, "smea", settingsManager::setBoolean);
        setupCheckBoxListener(smeaDynCheckBox, "smeaDyn", settingsManager::setBoolean);
        setupSliderListener(strengthSlider, "strength", settingsManager::setDouble);
        setupTextFieldListener(extraNoiseSeedField, "extraNoiseSeed", (key, value) -> settingsManager.setLong(key, Long.parseLong(value)));

        positivePromptArea.getPromptTextArea().textProperty().addListener((observable, oldValue, newValue) ->
                updatePromptPreview(newValue, positivePromptPreviewArea));
        negativePromptArea.getPromptTextArea().textProperty().addListener((observable, oldValue, newValue) ->
                updatePromptPreview(newValue, negativePromptPreviewArea));
    }

    private void setupTextFieldListener(TextField textField, String key, BiConsumer<String, String> setter) {
        textField.textProperty().addListener((obs, oldVal, newVal) -> setter.accept(key, newVal));
    }

    private void setupComboBoxListener(ComboBox<String> comboBox, String key, BiConsumer<String, String> setter) {
        comboBox.valueProperty().addListener((obs, oldVal, newVal) -> setter.accept(key, newVal));
    }

    private void setupPromptAreaListener(PromptArea promptArea, String key, BiConsumer<String, String> setter) {
        promptArea.getPromptTextArea().textProperty().addListener((obs, oldVal, newVal) -> setter.accept(key, newVal));
    }

    private void setupCheckBoxListener(CheckBox checkBox, String key, BiConsumer<String, Boolean> setter) {
        checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> setter.accept(key, newVal));
    }

    private void setupSliderListener(Slider slider, String key, BiConsumer<String, Double> setter) {
        slider.valueProperty().addListener((obs, oldVal, newVal) -> setter.accept(key, newVal.doubleValue()));
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