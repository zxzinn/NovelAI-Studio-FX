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
    @FXML private Tab generatorTab;
    @FXML private Tab Img2ImgTab;
    @FXML private Tab fileManagerTab;
    @FXML private BorderPane generatorTabContent;
    @FXML private BorderPane Img2ImgTabContent;
    @FXML private BorderPane fileManagerTabContent;

    private ImageGeneratorController generatorTabController;
    private Img2ImgGeneratorController img2ImgGeneratorController;
    private FileManagerController fileManagerTabController;
    private Window mainWindow;

    @FXML
    private void initialize() {
        Platform.runLater(() -> {
            try {
                FXMLLoader generatorLoader = new FXMLLoader(getClass().getResource("ImageGenerator.fxml"));
                generatorTabContent = generatorLoader.load();
                generatorTabController = generatorLoader.getController();

                FXMLLoader img2ImgLoader = new FXMLLoader(getClass().getResource("Img2ImgGenerator.fxml"));
                Img2ImgTabContent = img2ImgLoader.load();
                img2ImgGeneratorController = img2ImgLoader.getController();

                FXMLLoader fileManagerLoader = new FXMLLoader(getClass().getResource("FileManager.fxml"));
                fileManagerTabContent = fileManagerLoader.load();
                fileManagerTabController = fileManagerLoader.getController();

                generatorTab.setContent(generatorTabContent);
                Img2ImgTab.setContent(Img2ImgTabContent);
                fileManagerTab.setContent(fileManagerTabContent);

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