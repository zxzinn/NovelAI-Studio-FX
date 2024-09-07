package com.zxzinn.novelai.controller.generation;

import com.google.inject.Inject;
import com.zxzinn.novelai.api.APIClient;
import com.zxzinn.novelai.api.GenerationPayload;
import com.zxzinn.novelai.component.*;
import com.zxzinn.novelai.model.GenerationResult;
import com.zxzinn.novelai.model.GenerationTask;
import com.zxzinn.novelai.service.filemanager.FilePreviewService;
import com.zxzinn.novelai.service.generation.*;
import com.zxzinn.novelai.service.ui.NotificationService;
import com.zxzinn.novelai.utils.common.PropertiesManager;
import com.zxzinn.novelai.utils.embed.EmbedFileManager;
import com.zxzinn.novelai.utils.embed.EmbedProcessor;
import com.zxzinn.novelai.utils.image.ImageUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import lombok.extern.log4j.Log4j2;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
public class GenerationController {
    private static final String GENERATE_BUTTON_CLASS = "generate-button";

    private final FilePreviewService filePreviewService;
    private final PropertiesManager propertiesManager;
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
    @FXML private ImagePreviewPane imagePreviewPane;
    @FXML private Button generateButton;
    @FXML private TitledPane txt2imgSettingsPane;
    @FXML private TitledPane img2imgSettingsPane;
    @FXML private CheckBox smeaCheckBox;
    @FXML private CheckBox smeaDynCheckBox;
    @FXML private Slider strengthSlider;
    @FXML private TextField extraNoiseSeedField;
    @FXML private Button uploadImageButton;

    private volatile boolean isGenerating = false;
    private volatile boolean stopRequested = false;
    private volatile boolean isStopping = false;
    private String base64Image;
    private AtomicInteger remainingGenerations;
    private final AtomicBoolean isInfiniteMode = new AtomicBoolean(false);
    private final GenerationTaskManager taskManager = GenerationTaskManager.getInstance();

    @Inject
    public GenerationController(FilePreviewService filePreviewService, PropertiesManager propertiesManager) {
        this.filePreviewService = filePreviewService;
        this.propertiesManager = propertiesManager;
        this.promptManager = new PromptManager(new EmbedProcessor());
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
        apiKeyField.setText(propertiesManager.getString("apiKey", ""));
        modelComboBox.setValue(propertiesManager.getString("model", "nai-diffusion-3"));
        widthField.setText(String.valueOf(propertiesManager.getInt("width", 832)));
        heightField.setText(String.valueOf(propertiesManager.getInt("height", 1216)));
        samplerComboBox.setValue(propertiesManager.getString("sampler", "k_euler"));
        stepsField.setText(String.valueOf(propertiesManager.getInt("steps", 28)));
        seedField.setText(String.valueOf(propertiesManager.getInt("seed", 0)));
        generateCountComboBox.setValue(propertiesManager.getString("generateCount", "1"));
        positivePromptArea.setPromptText(propertiesManager.getString("positivePrompt", ""));
        negativePromptArea.setPromptText(propertiesManager.getString("negativePrompt", ""));
        outputDirectoryField.setText(propertiesManager.getString("outputDirectory", "output"));
        generationModeComboBox.setValue(propertiesManager.getString("generationMode", "Text2Image"));
        smeaCheckBox.setSelected(propertiesManager.getBoolean("smea", true));
        smeaDynCheckBox.setSelected(propertiesManager.getBoolean("smeaDyn", false));
        strengthSlider.setValue(propertiesManager.getDouble("strength", 0.5));
        extraNoiseSeedField.setText(String.valueOf(propertiesManager.getLong("extraNoiseSeed", 0)));
        ratioField.setText(String.valueOf(propertiesManager.getInt("ratio", 7)));
        countField.setText(String.valueOf(propertiesManager.getInt("count", 1)));
    }

    private void setupListeners() {
        apiKeyField.textProperty().addListener((obs, oldVal, newVal) -> propertiesManager.setString("apiKey", newVal));
        modelComboBox.valueProperty().addListener((obs, oldVal, newVal) -> propertiesManager.setString("model", newVal));
        widthField.textProperty().addListener((obs, oldVal, newVal) -> propertiesManager.setInt("width", Integer.parseInt(newVal)));
        heightField.textProperty().addListener((obs, oldVal, newVal) -> propertiesManager.setInt("height", Integer.parseInt(newVal)));
        samplerComboBox.valueProperty().addListener((obs, oldVal, newVal) -> propertiesManager.setString("sampler", newVal));
        stepsField.textProperty().addListener((obs, oldVal, newVal) -> propertiesManager.setInt("steps", Integer.parseInt(newVal)));
        seedField.textProperty().addListener((obs, oldVal, newVal) -> propertiesManager.setInt("seed", Integer.parseInt(newVal)));
        generateCountComboBox.valueProperty().addListener((obs, oldVal, newVal) -> propertiesManager.setString("generateCount", newVal));
        positivePromptArea.getPromptTextArea().textProperty().addListener((obs, oldVal, newVal) -> {
            propertiesManager.setString("positivePrompt", newVal);
            promptManager.updatePromptPreview(newVal, positivePromptPreviewArea, true);
        });
        negativePromptArea.getPromptTextArea().textProperty().addListener((obs, oldVal, newVal) -> {
            propertiesManager.setString("negativePrompt", newVal);
            promptManager.updatePromptPreview(newVal, negativePromptPreviewArea, false);
        });
        outputDirectoryField.textProperty().addListener((obs, oldVal, newVal) -> propertiesManager.setString("outputDirectory", newVal));
        generationModeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> propertiesManager.setString("generationMode", newVal));
        smeaCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> propertiesManager.setBoolean("smea", newVal));
        smeaDynCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> propertiesManager.setBoolean("smeaDyn", newVal));
        strengthSlider.valueProperty().addListener((obs, oldVal, newVal) -> propertiesManager.setDouble("strength", newVal.doubleValue()));
        extraNoiseSeedField.textProperty().addListener((obs, oldVal, newVal) -> propertiesManager.setLong("extraNoiseSeed", Long.parseLong(newVal)));
        ratioField.textProperty().addListener((obs, oldVal, newVal) -> propertiesManager.setInt("ratio", Integer.parseInt(newVal)));
        countField.textProperty().addListener((obs, oldVal, newVal) -> propertiesManager.setInt("count", Integer.parseInt(newVal)));
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

    private void updatePromptPreviews() {
        if (!promptManager.isPositivePromptLocked()) {
            promptManager.refreshPromptPreview(positivePromptArea, positivePromptPreviewArea, true);
        }
        if (!promptManager.isNegativePromptLocked()) {
            promptManager.refreshPromptPreview(negativePromptArea, negativePromptPreviewArea, false);
        }
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
        updateButtonState(true);

        int maxCount = getMaxCount();
        isInfiniteMode.set(maxCount == Integer.MAX_VALUE);
        remainingGenerations = new AtomicInteger(isInfiniteMode.get() ? Integer.MAX_VALUE : maxCount);

        generateNextImage();
    }

    private void generateNextImage() {
        if (stopRequested || (!isInfiniteMode.get() && remainingGenerations.get() <= 0)) {
            finishGeneration();
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                GenerationPayload payload = createGenerationPayload();
                GenerationTask task = new GenerationTask(payload, apiKeyField.getText());

                taskManager.submitTask(task)
                        .thenAccept(this::handleGenerationResult);
            } catch (Exception e) {
                log.error("Error creating generation task: ", e);
                finishGeneration();
            }
        });
    }

    private void handleGenerationResult(GenerationResult result) {
        if (result.isSuccess()) {
            Platform.runLater(() -> {
                handleGeneratedImage(result.getImageData());
                updatePromptPreviews();
                if (!isInfiniteMode.get()) {
                    remainingGenerations.decrementAndGet();
                }
                generateNextImage();
            });
        } else {
            Platform.runLater(() -> {
                NotificationService.showNotification("圖像生成失敗: " + result.getErrorMessage());
                finishGeneration();
            });
        }
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

    private void handleGeneratedImage(byte[] imageData) {
        Image image = new Image(new ByteArrayInputStream(imageData));
        saveImageToFile(imageData).ifPresent(imageFile -> {
            imagePreviewPane.updatePreview(imageFile);
            historyImagesPane.addImage(image, imageFile);
            NotificationService.showNotification("圖像生成成功！");
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