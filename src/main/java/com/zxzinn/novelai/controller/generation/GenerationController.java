package com.zxzinn.novelai.controller.generation;

import com.google.inject.Inject;
import com.zxzinn.novelai.api.GenerationPayload;
import com.zxzinn.novelai.component.*;
import com.zxzinn.novelai.model.GenerationResult;
import com.zxzinn.novelai.model.GenerationTask;
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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
public class GenerationController {
    private static final String GENERATE_BUTTON_CLASS = "generate-button";
    private static final String GENERATE_BUTTON_GENERATING_CLASS = "generate-button-generating";
    private static final String GENERATE_BUTTON_STOP_CLASS = "generate-button-stop";

    private final PropertiesManager propertiesManager;
    private final PromptManager promptManager;
    private final GenerationTaskManager taskManager;

    private final UIComponents uiComponents;
    private final GenerationState generationState;

    @Inject
    public GenerationController( ) {
        this.propertiesManager = PropertiesManager.getInstance();
        this.promptManager = new PromptManager(new EmbedProcessor());
        this.taskManager = GenerationTaskManager.getInstance();
        this.uiComponents = new UIComponents();
        this.generationState = new GenerationState();
        initializeUIComponents(); // 新增這行
    }

    private void initializeUIComponents() {
        uiComponents.apiSettingsPane = new ApiSettingsPane();
        uiComponents.outputSettingsPane = new OutputSettingsPane();
        uiComponents.samplingSettingsPane = new SamplingSettingsPane();
        uiComponents.text2ImageSettingsPane = new Text2ImageSettingsPane();
        uiComponents.image2ImageSettingsPane = new Image2ImageSettingsPane();
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
        settingsContent.getChildren().addAll(
                uiComponents.apiSettingsPane,
                uiComponents.outputSettingsPane,
                uiComponents.samplingSettingsPane,
                uiComponents.text2ImageSettingsPane,
                uiComponents.image2ImageSettingsPane
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

        uiComponents.previewContainer = new StackPane();
        uiComponents.previewContainer.getStyleClass().add("preview-container");
        VBox.setVgrow(uiComponents.previewContainer, Priority.ALWAYS);

        mainContent.getChildren().addAll(positivePromptBox, negativePromptBox, uiComponents.previewContainer);

        uiComponents.historyImagesPane = new HistoryImagesPane();
        uiComponents.historyImagesPane.setPrefWidth(100);
        HBox.setHgrow(uiComponents.historyImagesPane, Priority.NEVER);

        centerPanel.getChildren().addAll(mainContent, uiComponents.historyImagesPane);

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
            uiComponents.positivePromptArea = promptArea;
            uiComponents.positivePromptControls = promptControls;
            uiComponents.positivePromptPreviewArea = promptPreviewArea;
        } else {
            uiComponents.negativePromptArea = promptArea;
            uiComponents.negativePromptControls = promptControls;
            uiComponents.negativePromptPreviewArea = promptPreviewArea;
        }

        return promptBox;
    }

    private HBox createControlBar() {
        HBox controlBar = new HBox(10);
        controlBar.setAlignment(Pos.CENTER);
        controlBar.getStyleClass().add("control-bar");

        uiComponents.generateButton = new Button("生成");
        uiComponents.generateButton.getStyleClass().add(GENERATE_BUTTON_CLASS);
        uiComponents.generateButton.setOnAction(event -> handleGenerateOrStop());
        HBox.setHgrow(uiComponents.generateButton, Priority.NEVER);

        uiComponents.generateCountComboBox = new ComboBox<>();
        uiComponents.generateCountComboBox.setPromptText("生成數量");
        uiComponents.generateCountComboBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(uiComponents.generateCountComboBox, Priority.ALWAYS);

        controlBar.getChildren().addAll(uiComponents.generateButton, uiComponents.generateCountComboBox);

        return controlBar;
    }

    private void initialize() {
        EmbedFileManager embedFileManager = new EmbedFileManager();
        embedFileManager.scanEmbedFiles();

        uiComponents.positivePromptArea.setEmbedFileManager(embedFileManager);
        uiComponents.negativePromptArea.setEmbedFileManager(embedFileManager);

        uiComponents.imagePreviewPane = new ImagePreviewPane();
        uiComponents.previewContainer.getChildren().add(uiComponents.imagePreviewPane);
        uiComponents.historyImagesPane.setOnImageClickHandler(this::handleHistoryImageClick);

        initializeFields();
        loadSettings();
        setupListeners();
        promptManager.setupPromptControls(uiComponents.positivePromptControls, uiComponents.negativePromptControls,
                uiComponents.positivePromptArea, uiComponents.negativePromptArea,
                uiComponents.positivePromptPreviewArea, uiComponents.negativePromptPreviewArea);
        updatePromptPreviews();
        setupGenerationModeComboBox();
    }

    private void initializeFields() {
        initializeGenerateCountComboBox();
        initializePromptAreas();
        initializePromptPreviewAreas();
    }

    private void initializeGenerateCountComboBox() {
        uiComponents.generateCountComboBox.getItems().addAll("1", "2", "3", "4", "無限");
        uiComponents.generateCountComboBox.setValue("1");
    }

    private void initializePromptAreas() {
        uiComponents.positivePromptArea.setPromptLabel("正面提示詞:");
        uiComponents.negativePromptArea.setPromptLabel("負面提示詞:");
    }

    private void initializePromptPreviewAreas() {
        uiComponents.positivePromptPreviewArea.setPreviewLabel("正面提示詞預覽");
        uiComponents.negativePromptPreviewArea.setPreviewLabel("負面提示詞預覽");
    }

    private void loadSettings() {
        uiComponents.positivePromptArea.setPromptText(propertiesManager.getString("positivePrompt", ""));
        uiComponents.negativePromptArea.setPromptText(propertiesManager.getString("negativePrompt", ""));
        uiComponents.generateCountComboBox.setValue(propertiesManager.getString("generateCount", "1"));
    }

    private void setupListeners() {
        uiComponents.generateCountComboBox.valueProperty().addListener((obs, oldVal, newVal) ->
                propertiesManager.setString("generateCount", newVal));
        uiComponents.positivePromptArea.getPromptTextArea().textProperty().addListener((obs, oldVal, newVal) -> {
            propertiesManager.setString("positivePrompt", newVal);
            promptManager.updatePromptPreview(newVal, uiComponents.positivePromptPreviewArea, true);
        });
        uiComponents.negativePromptArea.getPromptTextArea().textProperty().addListener((obs, oldVal, newVal) -> {
            propertiesManager.setString("negativePrompt", newVal);
            promptManager.updatePromptPreview(newVal, uiComponents.negativePromptPreviewArea, false);
        });
    }

    private void setupGenerationModeComboBox() {
        uiComponents.generationModeComboBox = uiComponents.apiSettingsPane.getGenerationModeComboBox();
        uiComponents.generationModeComboBox.setOnAction(event -> updateSettingsPanes());
        updateSettingsPanes();
    }

    private void updateSettingsPanes() {
        boolean isText2Image = "Text2Image".equals(uiComponents.generationModeComboBox.getValue());
        uiComponents.text2ImageSettingsPane.setVisible(isText2Image);
        uiComponents.text2ImageSettingsPane.setManaged(isText2Image);
        uiComponents.image2ImageSettingsPane.setVisible(!isText2Image);
        uiComponents.image2ImageSettingsPane.setManaged(!isText2Image);
    }

    private void handleHistoryImageClick(File imageFile) {
        uiComponents.imagePreviewPane.updatePreview(imageFile);
    }

    private void updatePromptPreviews() {
        if (!promptManager.isPositivePromptLocked()) {
            promptManager.refreshPromptPreview(uiComponents.positivePromptArea, uiComponents.positivePromptPreviewArea, true);
        }
        if (!promptManager.isNegativePromptLocked()) {
            promptManager.refreshPromptPreview(uiComponents.negativePromptArea, uiComponents.negativePromptPreviewArea, false);
        }
    }

    private void handleGenerateOrStop() {
        if (!generationState.isGenerating && !generationState.isStopping) {
            startGeneration();
        } else if (generationState.isGenerating && !generationState.isStopping) {
            stopGeneration();
        }
    }

    private void startGeneration() {
        if ("Image2Image".equals(uiComponents.generationModeComboBox.getValue()) && !isImageUploaded()) {
            NotificationService.showNotification("請先上傳一張圖片");
            return;
        }

        generationState.isGenerating = true;
        generationState.stopRequested = false;
        generationState.isStopping = false;
        updateButtonState(true);

        int maxCount = getMaxCount();
        generationState.isInfiniteMode.set(maxCount == Integer.MAX_VALUE);
        generationState.remainingGenerations = new AtomicInteger(generationState.isInfiniteMode.get() ? Integer.MAX_VALUE : maxCount);

        generateNextImage();
    }

    private boolean isImageUploaded() {
        return uiComponents.image2ImageSettingsPane.getBase64Image() != null &&
                !uiComponents.image2ImageSettingsPane.getBase64Image().isEmpty();
    }

    private void generateNextImage() {
        if (generationState.stopRequested || (!generationState.isInfiniteMode.get() && generationState.remainingGenerations.get() <= 0)) {
            finishGeneration();
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                GenerationPayload payload = createGenerationPayload();
                GenerationTask task = new GenerationTask(payload, uiComponents.apiSettingsPane.getApiKey());

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
                if (!generationState.isInfiniteMode.get()) {
                    generationState.remainingGenerations.decrementAndGet();
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
        generationState.stopRequested = true;
        generationState.isStopping = true;
        updateButtonState(false);
    }

    private void updateButtonState(boolean generating) {
        Platform.runLater(() -> {
            uiComponents.generateButton.getStyleClass().removeAll(GENERATE_BUTTON_CLASS, GENERATE_BUTTON_GENERATING_CLASS, GENERATE_BUTTON_STOP_CLASS);
            if (generating) {
                uiComponents.generateButton.getStyleClass().add(GENERATE_BUTTON_GENERATING_CLASS);
                uiComponents.generateButton.setText("停止");
            } else if (generationState.isStopping) {
                uiComponents.generateButton.getStyleClass().add(GENERATE_BUTTON_STOP_CLASS);
                uiComponents.generateButton.setText("停止中...");
            } else {
                uiComponents.generateButton.getStyleClass().add(GENERATE_BUTTON_CLASS);
                uiComponents.generateButton.setText("生成");
            }
            uiComponents.generateButton.setDisable(generationState.isStopping);
        });
    }

    private void handleGeneratedImage(byte[] imageData) {
        Image image = new Image(new ByteArrayInputStream(imageData));
        saveImageToFile(imageData).ifPresent(imageFile -> {
            uiComponents.imagePreviewPane.updatePreview(imageFile);
            uiComponents.historyImagesPane.addImage(image, imageFile);
            NotificationService.showNotification("圖像生成成功！");
        });
    }

    private Optional<File> saveImageToFile(byte[] imageData) {
        return ImageUtils.saveImage(imageData, uiComponents.outputSettingsPane.getOutputDirectory());
    }

    private int getMaxCount() {
        String selectedCount = uiComponents.generateCountComboBox.getValue();
        return "無限".equals(selectedCount) ? Integer.MAX_VALUE : Integer.parseInt(selectedCount);
    }

    private void finishGeneration() {
        generationState.isGenerating = false;
        generationState.isStopping = false;
        updateButtonState(false);
    }

    private GenerationPayload createGenerationPayload() {
        GenerationPayload payload = new GenerationPayload();
        GenerationPayload.GenerationParameters params = new GenerationPayload.GenerationParameters();
        payload.setParameters(params);

        payload.setInput(uiComponents.positivePromptPreviewArea.getPreviewText());
        payload.setModel(uiComponents.apiSettingsPane.getModel());

        String generationMode = uiComponents.generationModeComboBox.getValue();
        if ("Text2Image".equals(generationMode)) {
            params.setSm(uiComponents.text2ImageSettingsPane.isSmea());
            params.setSm_dyn(uiComponents.text2ImageSettingsPane.isSmeaDyn());
            payload.setAction("generate");
        } else {
            payload.setAction("img2img");
            params.setStrength(uiComponents.image2ImageSettingsPane.getStrength());
            params.setNoise(uiComponents.image2ImageSettingsPane.getNoise());
            params.setImage(uiComponents.image2ImageSettingsPane.getBase64Image());
            params.setExtra_noise_seed(uiComponents.image2ImageSettingsPane.getExtraNoiseSeed());
        }

        params.setParams_version(1);
        params.setWidth(uiComponents.outputSettingsPane.getOutputWidth());
        params.setHeight(uiComponents.outputSettingsPane.getOutputHeight());
        params.setScale(uiComponents.outputSettingsPane.getRatio());
        params.setSampler(uiComponents.samplingSettingsPane.getSampler());
        params.setSteps(uiComponents.samplingSettingsPane.getSteps());
        params.setN_samples(uiComponents.outputSettingsPane.getCount());
        params.setSeed(uiComponents.samplingSettingsPane.getSeed());

        params.setNegative_prompt(uiComponents.negativePromptPreviewArea.getPreviewText());
        return payload;
    }

    private static class UIComponents {
        ApiSettingsPane apiSettingsPane;
        OutputSettingsPane outputSettingsPane;
        SamplingSettingsPane samplingSettingsPane;
        Text2ImageSettingsPane text2ImageSettingsPane;
        Image2ImageSettingsPane image2ImageSettingsPane;

        ComboBox<String> generationModeComboBox;
        PromptArea positivePromptArea;
        PromptArea negativePromptArea;
        PromptPreviewArea positivePromptPreviewArea;
        PromptPreviewArea negativePromptPreviewArea;
        PromptControls positivePromptControls;
        PromptControls negativePromptControls;
        ComboBox<String> generateCountComboBox;
        StackPane previewContainer;
        HistoryImagesPane historyImagesPane;
        ImagePreviewPane imagePreviewPane;
        Button generateButton;
    }

    private static class GenerationState {
        volatile boolean isGenerating = false;
        volatile boolean stopRequested = false;
        volatile boolean isStopping = false;
        AtomicInteger remainingGenerations;
        final AtomicBoolean isInfiniteMode = new AtomicBoolean(false);
    }
}