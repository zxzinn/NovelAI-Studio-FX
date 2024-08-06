package com.zxzinn.novelai;

import com.zxzinn.novelai.controller.FileManagerController;
import com.zxzinn.novelai.controller.ImageGeneratorController;
import com.zxzinn.novelai.controller.Img2ImgGeneratorController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;

import java.io.IOException;

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
        Platform.runLater(() -> {
            try {
                FXMLLoader generatorLoader = new FXMLLoader(getClass().getResource("ImageGenerator.fxml"));
                generatorLoader.load();
                generatorTabController = generatorLoader.getController();

                FXMLLoader img2ImgLoader = new FXMLLoader(getClass().getResource("Img2ImgGenerator.fxml"));
                img2ImgLoader.load();
                img2ImgGeneratorController = img2ImgLoader.getController();

                FXMLLoader fileManagerLoader = new FXMLLoader(getClass().getResource("FileManager.fxml"));
                fileManagerLoader.load();
                fileManagerTabController = fileManagerLoader.getController();

                generatorTab.setOnSelectionChanged(event -> {
                    if (generatorTab.isSelected()) {
                        mainWindow = generatorTab.getContent().getScene().getWindow();
                        generatorTabController.setMainWindow(mainWindow);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}