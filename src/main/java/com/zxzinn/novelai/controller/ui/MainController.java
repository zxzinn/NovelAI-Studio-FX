package com.zxzinn.novelai.controller.ui;

import com.zxzinn.novelai.api.APIClient;
import com.zxzinn.novelai.controller.filemanager.FileManagerController;
import com.zxzinn.novelai.controller.generation.img2img.Img2ImgGeneratorController;
import com.zxzinn.novelai.controller.generation.text2img.ImageGeneratorController;
import com.zxzinn.novelai.service.filemanager.FileManagerService;
import com.zxzinn.novelai.service.filemanager.FilePreviewService;
import com.zxzinn.novelai.service.filemanager.ImageProcessingService;
import com.zxzinn.novelai.service.filemanager.MetadataService;
import com.zxzinn.novelai.service.generation.ImageGenerationService;
import com.zxzinn.novelai.service.ui.AlertService;
import com.zxzinn.novelai.service.ui.WindowService;
import com.zxzinn.novelai.utils.common.SettingsManager;
import com.zxzinn.novelai.utils.embed.EmbedProcessor;
import com.zxzinn.novelai.utils.image.ImageUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;

@Log4j2
public class MainController {
    @FXML private TabPane mainTabPane;
    @FXML private Tab generatorTab;
    @FXML private Tab img2ImgTab;
    @FXML private Tab fileManagerTab;
    @FXML private Button minimizeButton;
    @FXML private Button maximizeButton;
    @FXML private Button closeButton;
    @FXML private VBox titleBar;

    private final SettingsManager settingsManager;
    private final APIClient apiClient;
    private final EmbedProcessor embedProcessor;
    private final ImageGenerationService imageGenerationService;
    private final ImageUtils imageUtils;
    private final WindowService windowService;
    private final FilePreviewService filePreviewService;

    public MainController(SettingsManager settingsManager, APIClient apiClient, EmbedProcessor embedProcessor,
                          ImageGenerationService imageGenerationService, ImageUtils imageUtils,
                          WindowService windowService, FilePreviewService filePreviewService) {
        this.settingsManager = settingsManager;
        this.apiClient = apiClient;
        this.embedProcessor = embedProcessor;
        this.imageGenerationService = imageGenerationService;
        this.imageUtils = imageUtils;
        this.windowService = windowService;
        this.filePreviewService = filePreviewService;
    }

    @FXML
    public void initialize() {
        setupWindowControls();
        loadTabContent();
        windowService.setupDraggableWindow(titleBar);
    }

    public void setStage(Stage stage) {
        windowService.setStage(stage);
        windowService.setupResizeableWindow();
    }

    private void setupWindowControls() {
        minimizeButton.setOnAction(event -> windowService.minimizeWindow());
        maximizeButton.setOnAction(event -> windowService.toggleMaximize());
        closeButton.setOnAction(event -> windowService.closeWindow());
    }

    private void loadTabContent() {
        try {
            loadGeneratorTab();
            loadImg2ImgTab();
            loadFileManagerTab();
        } catch (IOException e) {
            log.error("載入標籤內容時發生錯誤", e);
        }
    }

    private void loadGeneratorTab() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/zxzinn/novelai/ImageGenerator.fxml"));
        loader.setControllerFactory(param -> new ImageGeneratorController(apiClient, embedProcessor, settingsManager,
                imageGenerationService, imageUtils, filePreviewService));
        BorderPane content = loader.load();
        generatorTab.setContent(content);
    }

    private void loadImg2ImgTab() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/zxzinn/novelai/Img2ImgGenerator.fxml"));
        loader.setControllerFactory(param -> new Img2ImgGeneratorController(apiClient, embedProcessor, settingsManager,
                imageGenerationService, imageUtils, filePreviewService));
        BorderPane content = loader.load();
        img2ImgTab.setContent(content);
    }

    private void loadFileManagerTab() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/zxzinn/novelai/FileManager.fxml"));

        FileManagerService fileManagerService = new FileManagerService(settingsManager);
        MetadataService metadataService = new MetadataService();
        ImageProcessingService imageProcessingService = new ImageProcessingService();
        AlertService alertService = new AlertService();

        FileManagerController controller = new FileManagerController(
                settingsManager,
                fileManagerService,
                filePreviewService,
                metadataService,
                imageProcessingService,
                alertService
        );

        loader.setController(controller);
        BorderPane content = loader.load();
        fileManagerTab.setContent(content);
    }
}