package com.zxzinn.novelai;

import com.zxzinn.novelai.controller.FileManagerController;
import com.zxzinn.novelai.controller.ImageGeneratorController;
import com.zxzinn.novelai.controller.Img2ImgGeneratorController;
import com.zxzinn.novelai.ui.TitleBarController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class MainController {
    @FXML private AnchorPane rootPane;
    @FXML private HBox titleBar;
    @FXML private VBox mainVBox;
    @FXML private Tab generatorTab;
    @FXML private Tab Img2ImgTab;
    @FXML private Tab fileManagerTab;
    @FXML private BorderPane generatorTabContent;
    @FXML private BorderPane Img2ImgTabContent;
    @FXML private BorderPane fileManagerTabContent;
    @FXML private Label titleLabel;
    @FXML private Button minimizeButton;
    @FXML private Button maximizeButton;
    @FXML private Button closeButton;

    private double xOffset = 0;
    private double yOffset = 0;

    private ImageGeneratorController generatorTabController;
    private Img2ImgGeneratorController img2ImgGeneratorController;
    private FileManagerController fileManagerTabController;

    @FXML
    private void initialize() {
        Platform.runLater(this::setupUI);
    }

    private void setupUI() {
        if (rootPane == null || titleBar == null) {
            System.err.println("錯誤：rootPane或titleBar為null。請檢查FXML中的fx:id設置是否正確。");
            return;
        }

        try {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            if (stage == null) {
                System.err.println("錯誤：無法獲取Stage。請確保已設置Scene。");
                return;
            }

            loadTabContent();
            setupTabListeners();
        } catch (Exception e) {
            e.printStackTrace();
        }
        setupTitleBarEvents();
    }

    private void loadTabContent() throws IOException {
        FXMLLoader generatorLoader = new FXMLLoader(getClass().getResource("ImageGenerator.fxml"));
        generatorTabContent = generatorLoader.load();
        generatorTabController = generatorLoader.getController();

        FXMLLoader img2ImgLoader = new FXMLLoader(getClass().getResource("Img2ImgGenerator.fxml"));
        Img2ImgTabContent = img2ImgLoader.load();
        img2ImgGeneratorController = img2ImgLoader.getController();

        FXMLLoader fileManagerLoader = new FXMLLoader(getClass().getResource("FileManager.fxml"));
        fileManagerTabContent = fileManagerLoader.load();
        fileManagerTabController = fileManagerLoader.getController();

        if (generatorTab != null) generatorTab.setContent(generatorTabContent);
        if (Img2ImgTab != null) Img2ImgTab.setContent(Img2ImgTabContent);
        if (fileManagerTab != null) fileManagerTab.setContent(fileManagerTabContent);
    }

    private void setupTabListeners() {
        if (generatorTab != null) {
            generatorTab.setOnSelectionChanged(event -> {
                if (generatorTab.isSelected() && generatorTabController != null) {
                    Stage stage = (Stage) rootPane.getScene().getWindow();
                    generatorTabController.setMainWindow(stage);
                }
            });
        }
    }

    private void setupTitleBarEvents() {
        minimizeButton.setOnAction(event -> ((Stage) rootPane.getScene().getWindow()).setIconified(true));
        maximizeButton.setOnAction(event -> toggleMaximize());
        closeButton.setOnAction(event -> closeApplication());

        titleBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        titleBar.setOnMouseDragged(event -> {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
        titleBar.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                toggleMaximize();
            }
        });
    }

    private void toggleMaximize() {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.setMaximized(!stage.isMaximized());
    }

    private void closeApplication() {
        Platform.exit();
        System.exit(0);
    }
}