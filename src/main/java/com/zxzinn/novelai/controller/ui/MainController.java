package com.zxzinn.novelai.controller.ui;

import com.google.inject.Inject;
import com.zxzinn.novelai.service.ui.WindowService;
import com.zxzinn.novelai.utils.common.TabFactory;
import com.zxzinn.novelai.utils.ui.UIManager;
import com.zxzinn.novelai.utils.ui.DragAndDropHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.File;

public class MainController {
    @FXML private BorderPane rootPane;
    @FXML private TabPane mainTabPane;
    @FXML private Button minimizeButton;
    @FXML private Button maximizeButton;
    @FXML private Button closeButton;
    @FXML private VBox titleBar;

    private final UIManager uiManager;
    private final TabFactory tabFactory;
    private final WindowService windowService;
    private final DragAndDropHandler dragAndDropHandler;

    @Inject
    public MainController(WindowService windowService, TabFactory tabFactory) {
        this.windowService = windowService;
        this.uiManager = new UIManager(windowService);
        this.tabFactory = tabFactory;
        this.dragAndDropHandler = new DragAndDropHandler(this::handleFileDrop);
    }

    @FXML
    public void initialize() {
        uiManager.setupWindowControls(minimizeButton, maximizeButton, closeButton, titleBar);
        uiManager.loadTabContent(mainTabPane, tabFactory);
    }

    public void setStage(Stage stage) {
        windowService.setupStage(stage);
        uiManager.setupStage(stage);

        // 创建StackPane并设置为根节点
        StackPane root = new StackPane(rootPane);
        stage.getScene().setRoot(root);

        // 启用拖放功能
        dragAndDropHandler.enableDragAndDrop(root);
    }

    private void handleFileDrop(File file) {
        // 处理拖放的文件
        System.out.println("File dropped: " + file.getAbsolutePath());
        // 这里可以添加更多的逻辑来处理拖放的文件
        // 例如,检查文件类型,更新UI,或者触发其他操作
    }
}