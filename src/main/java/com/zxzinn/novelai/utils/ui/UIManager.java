package com.zxzinn.novelai.utils.ui;

import com.google.inject.Inject;
import com.zxzinn.novelai.service.ui.WindowService;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

@Log4j2
public class UIManager {
    private final WindowService windowService;

    @Inject
    public UIManager(WindowService windowService) {
        this.windowService = windowService;
    }

    public void setupWindowControls(@NotNull Button minimizeButton, @NotNull Button maximizeButton, @NotNull Button closeButton, VBox titleBar) {
        minimizeButton.setOnAction(event -> windowService.minimizeWindow());
        maximizeButton.setOnAction(event -> windowService.toggleMaximize());
        closeButton.setOnAction(event -> windowService.closeWindow());
        windowService.setupDraggableWindow(titleBar);
    }

    public void setupStage(Stage stage) {
        windowService.setStage(stage);
        windowService.setupResizeableWindow();
    }
}