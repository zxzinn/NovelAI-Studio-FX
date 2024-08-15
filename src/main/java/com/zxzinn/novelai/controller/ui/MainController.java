package com.zxzinn.novelai.controller.ui;

import com.zxzinn.novelai.api.APIClient;
import com.zxzinn.novelai.service.filemanager.FileManagerService;
import com.zxzinn.novelai.service.filemanager.FilePreviewService;
import com.zxzinn.novelai.service.filemanager.MetadataService;
import com.zxzinn.novelai.service.generation.ImageGenerationService;
import com.zxzinn.novelai.service.ui.AlertService;
import com.zxzinn.novelai.service.ui.WindowService;
import com.zxzinn.novelai.utils.common.SettingsManager;
import com.zxzinn.novelai.utils.common.TabFactory;
import com.zxzinn.novelai.utils.embed.EmbedProcessor;
import com.zxzinn.novelai.utils.image.ImageUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;

@Log4j2
public class MainController {
    @FXML private TabPane mainTabPane;
    @FXML private Button minimizeButton;
    @FXML private Button maximizeButton;
    @FXML private Button closeButton;
    @FXML private VBox titleBar;

    private final WindowService windowService;
    private final TabFactory tabFactory;

    public MainController(SettingsManager settingsManager, APIClient apiClient, EmbedProcessor embedProcessor,
                          ImageGenerationService imageGenerationService, ImageUtils imageUtils,
                          WindowService windowService, FilePreviewService filePreviewService,
                          FileManagerService fileManagerService, MetadataService metadataService,
                          AlertService alertService) {
        this.windowService = windowService;
        this.tabFactory = TabFactory.getInstance(settingsManager, apiClient, embedProcessor, imageGenerationService,
                imageUtils, filePreviewService, fileManagerService, metadataService, alertService);
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
            mainTabPane.getTabs().addAll(
                    tabFactory.createGeneratorTab(),
                    tabFactory.createImg2ImgTab(),
                    tabFactory.createFileManagerTab()
            );
        } catch (IOException e) {
            log.error("載入標籤內容時發生錯誤", e);
        }
    }
}