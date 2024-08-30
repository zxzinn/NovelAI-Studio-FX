package com.zxzinn.novelai.controller.ui;

import com.google.inject.Inject;
import com.zxzinn.novelai.service.ui.WindowService;
import com.zxzinn.novelai.utils.common.TabFactory;
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
    private final WindowService windowService;

    @Inject
    public MainController(WindowService windowService, TabFactory tabFactory) {
        this.windowService = windowService;
        this.uiManager = new UIManager(windowService);
        this.tabFactory = tabFactory;
    }

    @FXML
    public void initialize() {
        uiManager.setupWindowControls(minimizeButton, maximizeButton, closeButton, titleBar);
        uiManager.loadTabContent(mainTabPane, tabFactory);
    }

    public void setStage(Stage stage) {
        windowService.setupStage(stage);
        uiManager.setupStage(stage);
    }
}