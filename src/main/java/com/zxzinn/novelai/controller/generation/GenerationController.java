package com.zxzinn.novelai.controller.generation;

import com.google.inject.Inject;
import com.zxzinn.novelai.api.APIClient;
import com.zxzinn.novelai.api.GenerationPayload;
import com.zxzinn.novelai.api.ImageGenerationPayload;
import com.zxzinn.novelai.api.Img2ImgGenerationPayload;
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
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import lombok.extern.log4j.Log4j2;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Objects;
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

    private ApiSettingsPane apiSettingsPane;
    private OutputSettingsPane outputSettingsPane;
    private SamplingSettingsPane samplingSettingsPane;
    private Text2ImageSettingsPane text2ImageSettingsPane;
    private Image2ImageSettingsPane image2ImageSettingsPane;

    private ComboBox<String> generationModeComboBox;
    private PromptArea positivePromptArea;
    private PromptArea negativePromptArea;
    private PromptPreviewArea positivePromptPreviewArea;
    private PromptPreviewArea negativePromptPreviewArea;
    private PromptControls positivePromptControls;
    private PromptControls negativePromptControls;
    private ComboBox<String> generateCountComboBox;
    private StackPane previewContainer;
    private HistoryImagesPane historyImagesPane;
    private ImagePreviewPane imagePreviewPane;
    private Button generateButton;

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

    public BorderPane createView() {
        BorderPane root = new BorderPane();
        root.getStylesheets().add(getClass().getResource("/com/zxzinn/novelai/css/style-purple.css").toExternalForm());

        VBox leftPanel = createLeftPanel();
        HBox centerPanel = createCenterPanel();

        root.setLeft(leftPanel);
        root.setCenter(centerPanel);

        initialize();

        return root;
    }

    private VBox createLeftPanel() {
        VBox leftPanel = new VBox(10);
        leftPanel.getStyleClass().add("settings-panel");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("side-panel");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox settingsContent = new VBox(10);
        apiSettingsPane = new ApiSettingsPane(propertiesManager);
        outputSettingsPane = new OutputSettingsPane(propertiesManager);
        samplingSettingsPane = new SamplingSettingsPane(propertiesManager);
        text2ImageSettingsPane = new Text2ImageSettingsPane(propertiesManager);
        image2ImageSettingsPane = new Image2ImageSettingsPane(propertiesManager);

        settingsContent.getChildren().addAll(
                apiSettingsPane,
                outputSettingsPane,
                samplingSettingsPane,
                text2ImageSettingsPane,
                image2ImageSettingsPane
        );

        scrollPane.setContent(settingsContent);

        HBox controlBar = createControlBar();

        leftPanel.getChildren().addAll(scrollPane, controlBar);

        return leftPanel;
    }

    private HBox createCenterPanel() {
        HBox centerPanel = new HBox(10);

        VBox mainContent = new VBox(10);
        mainContent.getStyleClass().add("main-content");
        HBox.setHgrow(mainContent, Priority.ALWAYS);

        HBox positivePromptBox = createPromptBox(true);
        HBox negativePromptBox = createPromptBox(false);

        previewContainer = new StackPane();
        previewContainer.getStyleClass().add("preview-container");
        VBox.setVgrow(previewContainer, Priority.ALWAYS);

        mainContent.getChildren().addAll(positivePromptBox, negativePromptBox, previewContainer);

        historyImagesPane = new HistoryImagesPane();
        historyImagesPane.setPrefWidth(100);
        HBox.setHgrow(historyImagesPane, Priority.NEVER);

        centerPanel.getChildren().addAll(mainContent, historyImagesPane);

        return centerPanel;
    }

    private HBox createPromptBox(boolean isPositive) {
        HBox promptBox = new HBox(10);
        HBox.setHgrow(promptBox, Priority.ALWAYS);

        PromptArea promptArea = new PromptArea();
        HBox.setHgrow(promptArea, Priority.ALWAYS);

        PromptControls promptControls = new PromptControls();

        PromptPreviewArea promptPreviewArea = new PromptPreviewArea();
        HBox.setHgrow(promptPreviewArea, Priority.ALWAYS);

        promptBox.getChildren().addAll(promptArea, promptControls, promptPreviewArea);

        if (isPositive) {
            positivePromptArea = promptArea;
            positivePromptControls = promptControls;
            positivePromptPreviewArea = promptPreviewArea;
        } else {
            negativePromptArea = promptArea;
            negativePromptControls = promptControls;
            negativePromptPreviewArea = promptPreviewArea;
        }

        return promptBox;
    }

    private HBox createControlBar() {
        HBox controlBar = new HBox(10);
        controlBar.setAlignment(Pos.CENTER);
        controlBar.getStyleClass().add("control-bar");

        generateButton = new Button("生成");
        generateButton.getStyleClass().add(GENERATE_BUTTON_CLASS);
        generateButton.setOnAction(event -> handleGenerateOrStop());
        HBox.setHgrow(generateButton, Priority.NEVER);

        generateCountComboBox = new ComboBox<>();
        generateCountComboBox.setPromptText("生成數量");
        generateCountComboBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(generateCountComboBox, Priority.ALWAYS);

        controlBar.getChildren().addAll(generateButton, generateCountComboBox);

        return controlBar;
    }

    private void initialize() {
        EmbedFileManager embedFileManager = new EmbedFileManager();
        embedFileManager.scanEmbedFiles();

        positivePromptArea.setEmbedFileManager(embedFileManager);
        negativePromptArea.setEmbedFileManager(embedFileManager);

        imagePreviewPane = new ImagePreviewPane(filePreviewService);
        previewContainer.getChildren().add(imagePreviewPane);
        historyImagesPane.setOnImageClickHandler(this::handleHistoryImageClick);

        initializeFields();
        loadSettings();
        setupListeners();
        promptManager.setupPromptControls(positivePromptControls, negativePromptControls,
                positivePromptArea, negativePromptArea, positivePromptPreviewArea, negativePromptPreviewArea);
        updatePromptPreviews();
        setupGenerationModeComboBox();
    }

    private void initializeFields() {
        initializeGenerateCountComboBox();
        initializePromptAreas();
        initializePromptPreviewAreas();
    }

    private void initializeGenerateCountComboBox() {
        generateCountComboBox.getItems().addAll("1", "2", "3", "4", "無限");
        generateCountComboBox.setValue("1");
    }

    private void initializePromptAreas() {
        positivePromptArea.setPromptLabel("正面提示詞:");
        negativePromptArea.setPromptLabel("負面提示詞:");
    }

    private void initializePromptPreviewAreas() {
        positivePromptPreviewArea.setPreviewLabel("正面提示詞預覽");
        negativePromptPreviewArea.setPreviewLabel("負面提示詞預覽");
    }

    private void loadSettings() {
        positivePromptArea.setPromptText(propertiesManager.getString("positivePrompt", ""));
        negativePromptArea.setPromptText(propertiesManager.getString("negativePrompt", ""));
        generateCountComboBox.setValue(propertiesManager.getString("generateCount", "1"));
    }

    private void setupListeners() {
        generateCountComboBox.valueProperty().addListener((obs, oldVal, newVal) -> propertiesManager.setString("generateCount", newVal));
        positivePromptArea.getPromptTextArea().textProperty().addListener((obs, oldVal, newVal) -> {
            propertiesManager.setString("positivePrompt", newVal);
            promptManager.updatePromptPreview(newVal, positivePromptPreviewArea, true);
        });
        negativePromptArea.getPromptTextArea().textProperty().addListener((obs, oldVal, newVal) -> {
            propertiesManager.setString("negativePrompt", newVal);
            promptManager.updatePromptPreview(newVal, negativePromptPreviewArea, false);
        });
    }

    private void setupGenerationModeComboBox() {
        generationModeComboBox = apiSettingsPane.getGenerationModeComboBox();
        generationModeComboBox.setOnAction(event -> updateSettingsPanes());
        updateSettingsPanes();
    }

    private void updateSettingsPanes() {
        boolean isText2Image = "Text2Image".equals(generationModeComboBox.getValue());
        text2ImageSettingsPane.setVisible(isText2Image);
        text2ImageSettingsPane.setManaged(isText2Image);
        image2ImageSettingsPane.setVisible(!isText2Image);
        image2ImageSettingsPane.setManaged(!isText2Image);
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
                GenerationTask task = new GenerationTask(payload, apiSettingsPane.getApiKey());

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
        return ImageUtils.saveImage(imageData, outputSettingsPane.getOutputDirectory());
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
        String generationMode = generationModeComboBox.getValue();
        GenerationPayload payload;

        if ("Text2Image".equals(generationMode)) {
            payload = new ImageGenerationPayload();
        } else {
            Img2ImgGenerationPayload img2ImgPayload = new Img2ImgGenerationPayload();
            Img2ImgGenerationPayload.Img2ImgGenerationParameters img2ImgParams = new Img2ImgGenerationPayload.Img2ImgGenerationParameters();
            img2ImgParams.setStrength(image2ImageSettingsPane.getStrength());
            img2ImgParams.setImage(base64Image);
            img2ImgParams.setExtra_noise_seed(image2ImageSettingsPane.getExtraNoiseSeed());
            img2ImgPayload.setParameters(img2ImgParams);
            payload = img2ImgPayload;
        }

        payload.setInput(positivePromptPreviewArea.getPreviewText());
        payload.setModel(apiSettingsPane.getModel());
        payload.setAction("generate");

        GenerationPayload.GenerationParameters params = new GenerationPayload.GenerationParameters();
        params.setWidth(outputSettingsPane.getOutputWidth());
        params.setHeight(outputSettingsPane.getOutputHeight());
        params.setScale(outputSettingsPane.getRatio());
        params.setSampler(samplingSettingsPane.getSampler());
        params.setSteps(samplingSettingsPane.getSteps());
        params.setN_samples(outputSettingsPane.getCount());
        params.setSeed(samplingSettingsPane.getSeed());
        params.setSm(text2ImageSettingsPane.isSmea());
        params.setSm_dyn(text2ImageSettingsPane.isSmeaDyn());
        params.setNegative_prompt(negativePromptPreviewArea.getPreviewText());

        payload.setParameters(params);

        return payload;
    }
}