package com.zxzinn.novelai.controller.generation;

import com.google.inject.Inject;
import com.zxzinn.novelai.component.*;
import com.zxzinn.novelai.model.GenerationResult;
import com.zxzinn.novelai.model.GenerationTask;
import com.zxzinn.novelai.model.UIComponentsData;
import com.zxzinn.novelai.service.generation.*;
import com.zxzinn.novelai.service.ui.NotificationService;
import com.zxzinn.novelai.utils.common.PropertiesManager;
import com.zxzinn.novelai.utils.embed.EmbedFileManager;
import com.zxzinn.novelai.utils.embed.EmbedProcessor;
import com.zxzinn.novelai.utils.image.ImageUtils;
import com.zxzinn.novelai.viewmodel.GenerationViewModel;
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

@Log4j2
public class GenerationController {
    private static final String GENERATE_BUTTON_CLASS = "generate-button";
    private static final String GENERATE_BUTTON_GENERATING_CLASS = "generate-button-generating";
    private static final String GENERATE_BUTTON_STOP_CLASS = "generate-button-stop";

    private final GenerationViewModel viewModel;
    private final UIComponents uiComponents;

    @Inject
    public GenerationController(GenerationViewModel viewModel) {
        this.viewModel = viewModel;
        this.uiComponents = new UIComponents();
        initializeUIComponents();
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

        PromptComponent promptComponent = new PromptComponent();
        HBox.setHgrow(promptComponent, Priority.ALWAYS);

        promptBox.getChildren().add(promptComponent);

        if (isPositive) {
            uiComponents.positivePromptComponent = promptComponent;
        } else {
            uiComponents.negativePromptComponent = promptComponent;
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

        uiComponents.positivePromptComponent.setEmbedFileManager(embedFileManager);
        uiComponents.negativePromptComponent.setEmbedFileManager(embedFileManager);

        uiComponents.imagePreviewPane = new ImagePreviewPane();
        uiComponents.previewContainer.getChildren().add(uiComponents.imagePreviewPane);
        uiComponents.historyImagesPane.setOnImageClickHandler(this::handleHistoryImageClick);

        initializeFields();
        loadSettings();
        setupListeners();
        viewModel.setupPromptControls(uiComponents.positivePromptComponent, uiComponents.negativePromptComponent);
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
        uiComponents.positivePromptComponent.setPromptLabel("正面提示詞:");
        uiComponents.negativePromptComponent.setPromptLabel("負面提示詞:");
    }

    private void initializePromptPreviewAreas() {
        uiComponents.positivePromptComponent.setPreviewLabel("正面提示詞預覽");
        uiComponents.negativePromptComponent.setPreviewLabel("負面提示詞預覽");
    }

    private void loadSettings() {
        viewModel.loadSettings(uiComponents.positivePromptComponent, uiComponents.negativePromptComponent, uiComponents.generateCountComboBox);
    }

    private void setupListeners() {
        viewModel.setupListeners(uiComponents.generateCountComboBox, uiComponents.positivePromptComponent, uiComponents.negativePromptComponent);
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
        viewModel.updatePromptPreviews(uiComponents.positivePromptComponent, uiComponents.negativePromptComponent);
    }

    private void handleGenerateOrStop() {
        if (!viewModel.isGenerating() && !viewModel.isStopping()) {
            startGeneration();
        } else if (viewModel.isGenerating() && !viewModel.isStopping()) {
            stopGeneration();
        }
    }

    private void startGeneration() {
        if ("Image2Image".equals(uiComponents.generationModeComboBox.getValue()) && !isImageUploaded()) {
            NotificationService.showNotification("請先上傳一張圖片");
            return;
        }

        viewModel.startGeneration();
        updateButtonState(true);

        generateNextImage();
    }

    private boolean isImageUploaded() {
        return uiComponents.image2ImageSettingsPane.getBase64Image() != null &&
                !uiComponents.image2ImageSettingsPane.getBase64Image().isEmpty();
    }

    private UIComponentsData collectUIData() {
        UIComponentsData data = new UIComponentsData();
        data.apiKey = uiComponents.apiSettingsPane.getApiKey();
        data.model = uiComponents.apiSettingsPane.getModel();
        data.generationMode = uiComponents.generationModeComboBox.getValue();
        data.positivePromptPreviewText = uiComponents.positivePromptComponent.getPreviewText();
        data.negativePromptPreviewText = uiComponents.negativePromptComponent.getPreviewText();
        data.smea = uiComponents.text2ImageSettingsPane.isSmea();
        data.smeaDyn = uiComponents.text2ImageSettingsPane.isSmeaDyn();
        data.strength = uiComponents.image2ImageSettingsPane.getStrength();
        data.noise = uiComponents.image2ImageSettingsPane.getNoise();
        data.base64Image = uiComponents.image2ImageSettingsPane.getBase64Image();
        data.extraNoiseSeed = uiComponents.image2ImageSettingsPane.getExtraNoiseSeed();
        data.outputWidth = uiComponents.outputSettingsPane.getOutputWidth();
        data.outputHeight = uiComponents.outputSettingsPane.getOutputHeight();
        data.ratio = uiComponents.outputSettingsPane.getRatio();
        data.sampler = uiComponents.samplingSettingsPane.getSampler();
        data.steps = uiComponents.samplingSettingsPane.getSteps();
        data.count = uiComponents.outputSettingsPane.getCount();
        data.seed = uiComponents.samplingSettingsPane.getSeed();
        return data;
    }

    private void generateNextImage() {
        if (viewModel.shouldStopGeneration()) {
            finishGeneration();
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                UIComponentsData uiData = collectUIData();
                GenerationTask task = viewModel.createGenerationTask(uiData);

                viewModel.submitTask(task)
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
                viewModel.decrementRemainingGenerations();
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
        viewModel.stopGeneration();
        updateButtonState(false);
    }

    private void updateButtonState(boolean generating) {
        Platform.runLater(() -> {
            uiComponents.generateButton.getStyleClass().removeAll(GENERATE_BUTTON_CLASS, GENERATE_BUTTON_GENERATING_CLASS, GENERATE_BUTTON_STOP_CLASS);
            if (generating) {
                uiComponents.generateButton.getStyleClass().add(GENERATE_BUTTON_GENERATING_CLASS);
                uiComponents.generateButton.setText("停止");
            } else if (viewModel.isStopping()) {
                uiComponents.generateButton.getStyleClass().add(GENERATE_BUTTON_STOP_CLASS);
                uiComponents.generateButton.setText("停止中...");
            } else {
                uiComponents.generateButton.getStyleClass().add(GENERATE_BUTTON_CLASS);
                uiComponents.generateButton.setText("生成");
            }
            uiComponents.generateButton.setDisable(viewModel.isStopping());
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

    private void finishGeneration() {
        viewModel.finishGeneration();
        updateButtonState(false);
    }

    public static class UIComponents {
        ApiSettingsPane apiSettingsPane;
        OutputSettingsPane outputSettingsPane;
        SamplingSettingsPane samplingSettingsPane;
        Text2ImageSettingsPane text2ImageSettingsPane;
        Image2ImageSettingsPane image2ImageSettingsPane;

        ComboBox<String> generationModeComboBox;
        PromptComponent positivePromptComponent;
        PromptComponent negativePromptComponent;
        ComboBox<String> generateCountComboBox;
        StackPane previewContainer;
        HistoryImagesPane historyImagesPane;
        ImagePreviewPane imagePreviewPane;
        Button generateButton;
    }
}