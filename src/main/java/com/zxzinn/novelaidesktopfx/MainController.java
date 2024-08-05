package com.zxzinn.novelaidesktopfx;

import javafx.fxml.FXML;
import javafx.scene.control.Tab;

public class MainController {
    @FXML private Tab generatorTab;
    @FXML private Tab fileManagerTab;
    @FXML private ImageGeneratorController generatorTabController;
    @FXML private FileManagerController fileManagerTabController;

    @FXML
    private void initialize() {
        // 如果需要在主控制器中進行一些初始化，可以在這裡添加代碼
    }
}