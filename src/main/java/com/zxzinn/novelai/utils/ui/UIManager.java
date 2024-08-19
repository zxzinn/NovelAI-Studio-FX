package com.zxzinn.novelai.utils.ui;

import com.zxzinn.novelai.service.ui.WindowService;
import com.zxzinn.novelai.utils.common.TabFactory;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class UIManager {
    private final WindowService windowService;

    public UIManager(WindowService windowService) {
        this.windowService = windowService;
    }

    public void setupWindowControls(Button minimizeButton, Button maximizeButton, Button closeButton, VBox titleBar) {
        minimizeButton.setOnAction(event -> windowService.minimizeWindow());
        maximizeButton.setOnAction(event -> windowService.toggleMaximize());
        closeButton.setOnAction(event -> windowService.closeWindow());
        windowService.setupDraggableWindow(titleBar);
    }

    public void setupStage(Stage stage) {
        windowService.setStage(stage);
        windowService.setupResizeableWindow();
    }

    public void loadTabContent(TabPane mainTabPane, TabFactory tabFactory) {
        try {
            mainTabPane.getTabs().addAll(
                    tabFactory.createUnifiedGeneratorTab(),
                    tabFactory.createFileManagerTab()
            );
        } catch (Exception e) {
            log.error(e);
        }
    }
}