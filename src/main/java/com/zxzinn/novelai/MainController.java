package com.zxzinn.novelai;

import com.zxzinn.novelai.controller.FileManagerController;
import com.zxzinn.novelai.controller.ImageGeneratorController;
import com.zxzinn.novelai.controller.Img2ImgGeneratorController;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;

public class MainController {
    public BorderPane generatorTabContent;
    public Tab Img2ImgTab;
    public BorderPane fileManagerTabContent;
    public BorderPane Img2ImgTabContent;
    @FXML private Tab generatorTab;
    @FXML private Tab fileManagerTab;
    @FXML private ImageGeneratorController generatorTabController;
    @FXML private Img2ImgGeneratorController img2ImgGeneratorController;
    @FXML private FileManagerController fileManagerTabController;
    private Window mainWindow;

    @FXML
    private void initialize() {
        generatorTab.setOnSelectionChanged(event -> {
            if (generatorTab.isSelected()) {
                mainWindow = generatorTab.getContent().getScene().getWindow();
                generatorTabController.setMainWindow(mainWindow);
            }
        });
    }
}