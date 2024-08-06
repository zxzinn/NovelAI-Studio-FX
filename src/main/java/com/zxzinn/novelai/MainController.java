package com.zxzinn.novelai;

import com.zxzinn.novelai.controller.FileManagerController;
import com.zxzinn.novelai.controller.ImageGeneratorController;
import com.zxzinn.novelai.controller.Img2ImgGeneratorController;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class MainController {
    private static final Logger logger = LogManager.getLogger(MainController.class);
    private static final long ANIMATION_DURATION = 20_000_000L; // 150ms in nanoseconds

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
    private Rectangle2D previousBounds;

    private ImageGeneratorController generatorTabController;
    private Img2ImgGeneratorController img2ImgGeneratorController;
    private FileManagerController fileManagerTabController;

    @FXML
    private void initialize() {
        Platform.runLater(this::setupUI);
    }

    private void setupUI() {
        if (rootPane == null || titleBar == null) {
            logger.error("錯誤：rootPane或titleBar為null。請檢查FXML中的fx:id設置是否正確。");
            return;
        }

        try {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            if (stage == null) {
                logger.error("錯誤：無法獲取Stage。請確保已設置Scene。");
                return;
            }

            loadTabContent();
            setupTabListeners();
        } catch (Exception e) {
            logger.error("設置UI時發生錯誤", e);
        }
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

    @FXML
    private void handleMousePressed(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    @FXML
    private void handleMouseDragged(MouseEvent event) {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.setX(event.getScreenX() - xOffset);
        stage.setY(event.getScreenY() - yOffset);
    }

    @FXML
    private void handleMouseClicked(MouseEvent event) {
        if (event.getClickCount() == 2) {
            handleMaximize();
        }
    }

    @FXML
    private void handleMinimize() {
        ((Stage) rootPane.getScene().getWindow()).setIconified(true);
    }

    @FXML
    private void handleMaximize() {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        Scene scene = stage.getScene();
        if (scene == null) return;

        if (!stage.isMaximized()) {
            previousBounds = new Rectangle2D(stage.getX(), stage.getY(), scene.getWidth(), scene.getHeight());
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            animateResize(screenBounds.getMinX(), screenBounds.getMinY(),
                    screenBounds.getWidth(), screenBounds.getHeight(), true);
        } else {
            animateResize(previousBounds.getMinX(), previousBounds.getMinY(),
                    previousBounds.getWidth(), previousBounds.getHeight(), false);
        }
    }

    @FXML
    private void handleClose() {
        Platform.exit();
        System.exit(0);
    }

    private void animateResize(double toX, double toY, double toWidth, double toHeight, boolean maximize) {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        final double startX = stage.getX();
        final double startY = stage.getY();
        final double startWidth = stage.getWidth();
        final double startHeight = stage.getHeight();
        final long startTime = System.nanoTime();

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                long elapsedTime = now - startTime;
                if (elapsedTime > ANIMATION_DURATION) {
                    stage.setX(toX);
                    stage.setY(toY);
                    stage.setWidth(toWidth);
                    stage.setHeight(toHeight);
                    stage.setMaximized(maximize);
                    this.stop();
                    logger.info("Animation completed in {} ms", elapsedTime / 1_000_000.0);
                } else {
                    double fraction = (double) elapsedTime / ANIMATION_DURATION;
                    stage.setX(startX + (toX - startX) * fraction);
                    stage.setY(startY + (toY - startY) * fraction);
                    stage.setWidth(startWidth + (toWidth - startWidth) * fraction);
                    stage.setHeight(startHeight + (toHeight - startHeight) * fraction);
                }
            }
        }.start();
    }
}