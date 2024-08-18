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
import com.zxzinn.novelai.utils.ui.UIManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainController {
    @FXML private TabPane mainTabPane;
    @FXML private Button minimizeButton;
    @FXML private Button maximizeButton;
    @FXML private Button closeButton;
    @FXML private VBox titleBar;

    private final UIManager uiManager;
    private final TabFactory tabFactory;

    public MainController(SettingsManager settingsManager, APIClient apiClient, EmbedProcessor embedProcessor,
                          ImageGenerationService imageGenerationService, ImageUtils imageUtils,
                          WindowService windowService, FilePreviewService filePreviewService,
                          FileManagerService fileManagerService, MetadataService metadataService,
                          AlertService alertService) {
        this.uiManager = new UIManager(windowService);
        this.tabFactory = TabFactory.getInstance(settingsManager, apiClient, embedProcessor, imageGenerationService,
                imageUtils, filePreviewService, fileManagerService, metadataService, alertService);
    }

    @FXML
    public void initialize() {
        uiManager.setupWindowControls(minimizeButton, maximizeButton, closeButton, titleBar);
        uiManager.loadTabContent(mainTabPane, tabFactory);
    }

    public void setStage(Stage stage) {
        uiManager.setupStage(stage);
    }
}